package com.badcompanydevz.ui

import com.badcompanydevz.data.LogcatReader
import com.badcompanydevz.model.AnalyticsEvent
import com.badcompanydevz.Strings
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import javax.swing.Icon
import javax.swing.JPanel

/** Owns the state (events, properties, filter) and wires the UI components together. */
class AnalyticsPanel : JPanel(BorderLayout()), Disposable {
    private val events = ArrayList<AnalyticsEvent>()
    private val properties = linkedMapOf<String, String>()
    private var filter = ""
    private val parameterFilters = linkedMapOf<String, String?>()
    private var paused = false
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
        val status = StatusIndicator(onReconnect = ::resync)
        Disposer.register(this, reader)
        Disposer.register(this, status)

        val header = JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(4, 10)
            toolbar = Toolbar(status, onPause = ::togglePause, onSync = ::resync, onClear = ::clear)
            add(toolbar, BorderLayout.NORTH)
            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                add(EventFilter { filter = it; refilter() }, BorderLayout.CENTER)
                parameterFiltersButton = IconButton(AllIcons.Actions.Properties, Strings["action_parameter_filters"], ::openParameterFilters)
                parameterFiltersButton.isEnabled = false
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
    }

    fun start() = reader.start()

    private fun currentFilter() = ParameterFilter(parameterFilters, filter)

    private fun visibleParams(event: AnalyticsEvent?) = currentFilter().visibleParams(event)

    private fun matches(event: AnalyticsEvent) = currentFilter().matches(event)

    private fun openParameterFilters() {
        val dialog = ParameterFiltersDialog(parameterFilters.keys, parameterFilters)
        if (dialog.showAndGet()) {
            parameterFilters.putAll(dialog.filters)
            resync()
            refilter()
        }
    }

    private fun addEvent(e: AnalyticsEvent) {
        events.add(0, e)
        parameterFiltersButton.isEnabled = true
        e.params.forEach { (name, _) -> if (!parameterFilters.containsKey(name)) parameterFilters[name] = "" }
        if (events.size > MAX_EVENTS) events.subList(MAX_EVENTS, events.size).clear()
        if (!paused && matches(e)) eventList.prepend(e, MAX_EVENTS)
    }

    private fun togglePause() {
        paused = !paused
        if (paused) eventsAtPause = events.toList()
        toolbar.pauseButton.icon = if (paused) resumeIcon else AllIcons.Actions.Pause
        toolbar.pauseButton.toolTipText = Strings[if (paused) "action_resume" else "action_pause"]
        refilter()
    }

    private fun refilter() {
        val current = currentFilter()
        eventList.setEvents((if (paused) eventsAtPause else events).filter(current::matches))
        parametersView.show(current.visibleParams(eventList.selectedValue))
    }

    private fun clear() {
        events.clear()
        eventsAtPause = emptyList()
        parameterFiltersButton.isEnabled = false
        parameterFilters.clear()
        eventList.clear()
    }

    private fun resync() {
        epoch++
        events.clear()
        eventsAtPause = emptyList()
        parameterFiltersButton.isEnabled = false
        eventList.clear()
        properties.clear()
        propertiesView.show(emptyList())
        reader.start()
    }

    private fun onUiThread(block: () -> Unit) {
        val e = epoch
        ApplicationManager.getApplication().invokeLater { if (e == epoch) block() }
    }

    override fun dispose() = Unit

    private companion object {
        const val MAX_EVENTS = 1000

        val resumeIcon = object : Icon {
            override fun getIconWidth() = AllIcons.Actions.Execute.iconWidth
            override fun getIconHeight() = AllIcons.Actions.Execute.iconHeight

            override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = Color.WHITE
                g2.fill(Polygon(
                    intArrayOf(x + iconWidth / 4, x + iconWidth * 3 / 4, x + iconWidth / 4),
                    intArrayOf(y + iconHeight / 6, y + iconHeight / 2, y + iconHeight * 5 / 6),
                    3
                ))
                g2.dispose()
            }
        }
    }
}
