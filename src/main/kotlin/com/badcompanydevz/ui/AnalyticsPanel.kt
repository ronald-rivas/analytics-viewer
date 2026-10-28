package com.badcompanydevz.ui

import com.badcompanydevz.data.LogcatReader
import com.badcompanydevz.model.AnalyticsEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

/** Owns the state (events, properties, filter) and wires the UI components together. */
class AnalyticsPanel : JPanel(BorderLayout()), Disposable {
    private val events = ArrayList<AnalyticsEvent>()
    private val properties = linkedMapOf<String, String>()
    private var filter = ""

    /** Bumped on clear/resync so UI updates queued by an older logcat session are dropped. */
    @Volatile
    private var epoch = 0

    private val parametersView = KeyValuePanel()
    private val propertiesView = KeyValuePanel()
    private val eventList = EventList { parametersView.show(it?.params.orEmpty()) }

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
            add(Toolbar(status, onSync = ::resync, onClear = ::clear), BorderLayout.NORTH)
            add(EventFilter { filter = it; refilter() }, BorderLayout.CENTER)
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

    private fun matches(e: AnalyticsEvent) =
        filter.isBlank() || e.title.contains(filter, true) || e.name.contains(filter, true)

    private fun addEvent(e: AnalyticsEvent) {
        events.add(0, e)
        if (events.size > MAX_EVENTS) events.subList(MAX_EVENTS, events.size).clear()
        if (matches(e)) eventList.prepend(e, MAX_EVENTS)
    }

    private fun refilter() = eventList.setEvents(events.filter(::matches))

    private fun clear() {
        events.clear()
        eventList.clear()
    }

    private fun resync() {
        epoch++
        clear()
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
    }
}
