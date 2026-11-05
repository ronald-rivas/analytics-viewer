package com.badcompanydevz.ui

import com.badcompanydevz.data.ExcelExporter
import com.badcompanydevz.data.LogcatReader
import com.badcompanydevz.model.AnalyticsEvent
import com.badcompanydevz.Strings
import com.intellij.icons.AllIcons
import com.intellij.ide.setToolTipText
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

/** Owns the state (events, properties, filter) and wires the UI components together. */
class AnalyticsPanel(val project: Project) : JPanel(BorderLayout()), Disposable {
    private val events = ArrayList<AnalyticsEvent>()
    private val properties = linkedMapOf<String, String>()
    private var filter = ""
    private val parameterFilters = linkedMapOf<String, String?>()
    private var paused = true
    private var adbConnected = false
    private var eventsAtPause: List<AnalyticsEvent> = emptyList()

    /** Bumped on clear/resync so UI updates queued by an older logcat session are dropped. */
    @Volatile
    private var epoch = 0

    private val parametersView = KeyValuePanel()
    private val propertiesView = KeyValuePanel()
    private lateinit var toolbar: Toolbar
    private lateinit var parameterFiltersButton: IconButton
    private val eventList = EventList { event ->
        parametersView.show(visibleParams(event))
    }

    private val reader = LogcatReader(
        onEvent = { e -> onUiThread { addEvent(e) } },
        onProperty = { k, v ->
            onUiThread {
                properties[k] = v
                propertiesView.show(properties.toList())
            }
        }
    )

    init {
        val status = StatusIndicator(onConnectionChanged = ::onConnectionChanged)
        Disposer.register(this, reader)
        Disposer.register(this, status)

        val header = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(4, 10)
            toolbar = Toolbar(status, onPause = ::togglePause, onSync = ::resync, onClear = ::clear, onSave = ::openSaveDialog)
            add(toolbar, BorderLayout.NORTH)
            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                add(EventFilter { filter = it; refilter() }, BorderLayout.CENTER)
                parameterFiltersButton = IconButton(AllIcons.Actions.Properties, Strings["action_parameter_filters"], ::openParameterFilters)
                add(parameterFiltersButton, BorderLayout.EAST)
            }, BorderLayout.CENTER)
        }
        val eventsPane = JBScrollPane(
            eventList, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        ).apply {
            border = JBUI.Borders.empty()
            viewportBorder = null
        }
        val left = JPanel(BorderLayout()).apply {
            add(header, BorderLayout.NORTH)
            add(eventsPane, BorderLayout.CENTER)
        }
        add(ThinSplitPane(left, FolderTabs(parametersView, propertiesView)), BorderLayout.CENTER)
        updateConnectionActions()
    }

    fun start() = reader.start()

    private fun currentFilter() = ParameterFilter(parameterFilters, filter)

    private fun visibleParams(event: AnalyticsEvent?) = currentFilter().visibleParams(event)

    private fun matches(event: AnalyticsEvent) = currentFilter().matches(event)

    private fun openParameterFilters() {
        val dialog = ParameterFiltersDialog(parameterFilters.keys, parameterFilters)
        if (dialog.showAndGet()) {
            parameterFilters.putAll(dialog.filters)
            refilter()
        }
    }

    private fun openSaveDialog() {
        val userHome = System.getProperty("user.home")
        val pluginName = Strings["plugin_name"]
        val projectName = project.name
        val defaultPath = "$userHome\\$pluginName\\$projectName"
        val dialog = SavedAnalyticsDialog(defaultPath)

        if (dialog.showAndGet()) {
            val dir = File(defaultPath)
            if (!dir.exists()) dir.mkdirs()
            val virtualDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir)

            val descriptor = FileSaverDescriptor(
                Strings["action_save"],
                "Choose the destination and name for the Excel file",
                "xlsx"
            )

            val fileWrapper = FileChooserFactory.getInstance()
                .createSaveFileDialog(descriptor, project)
                .save(virtualDir, "$projectName-analytics.xlsx")

            if (fileWrapper != null) {
                val finalFile = fileWrapper.file
                val applyFilter = dialog.isFiltered
                val listToExport = if (paused) eventsAtPause else events
                val currentFilter = currentFilter()
                val filteredList = if (applyFilter) {
                    listToExport.filter(currentFilter::matches)
                } else {
                    listToExport
                }

                try {
                    ExcelExporter.export(finalFile, filteredList, properties)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addEvent(e: AnalyticsEvent) {
        events.add(0, e)
        e.params.forEach { (name, _) -> if (!parameterFilters.containsKey(name)) parameterFilters[name] = "" }
        if (events.size > MAX_EVENTS) events.subList(MAX_EVENTS, events.size).clear()
        if (!paused && matches(e)) eventList.prepend(e, MAX_EVENTS)
    }

    private fun togglePause() {
        if (!adbConnected) return
        paused = !paused
        if (paused) eventsAtPause = events.toList()
        toolbar.pauseButton.icon = if (paused) Toolbar.runIcon else AllIcons.Actions.Pause
        toolbar.pauseButton.setToolTipText(HtmlChunk.text(Strings[if (paused) "action_run" else "action_pause"]))
        if (!paused) refilter()
    }

    private fun refilter() {
        val current = currentFilter()
        eventList.setEvents((if (paused) eventsAtPause else events).filter(current::matches))
        parametersView.show(current.visibleParams(eventList.selectedValue))
    }

    private fun clear() {
        events.clear()
        eventsAtPause = emptyList()
        eventList.clear()
    }

    private fun resync() {
        if (!adbConnected) return
        epoch++
        events.clear()
        eventsAtPause = emptyList()
        eventList.clear()
        properties.clear()
        propertiesView.show(emptyList())
        reader.start()
    }

    private fun onConnectionChanged(connected: Boolean) {
        adbConnected = connected
        if (!connected) {
            epoch++
            reader.stop()
            paused = true
            clear()
            parameterFilters.clear()
            properties.clear()
            propertiesView.show(emptyList())
        } else {
            resync()
        }
        updateConnectionActions()
        refilter()
    }

    private fun updateConnectionActions() {
        toolbar.pauseButton.isEnabled = adbConnected
        toolbar.syncButton.isEnabled = adbConnected
        toolbar.pauseButton.icon = if (paused) Toolbar.runIcon else AllIcons.Actions.Pause
        toolbar.pauseButton.setToolTipText(HtmlChunk.text(Strings[if (paused) "action_run" else "action_pause"]))
    }

    private fun onUiThread(block: () -> Unit) {
        val e = epoch
        ApplicationManager.getApplication().invokeLater { if (e == epoch) block() }
    }

    override fun dispose() = Unit

    private companion object {
        const val MAX_EVENTS = 1000
    }
}
