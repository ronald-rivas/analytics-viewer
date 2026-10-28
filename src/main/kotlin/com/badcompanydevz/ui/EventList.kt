package com.badcompanydevz.ui

import com.badcompanydevz.model.AnalyticsEvent
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.plaf.basic.BasicListUI

/** List of events drawn as buttons; [onSelect] fires only when the selected event actually changes. */
class EventList(private val onSelect: (AnalyticsEvent?) -> Unit) : JBList<AnalyticsEvent>(DefaultListModel()) {
    private var shown: AnalyticsEvent? = null

    private val events: DefaultListModel<AnalyticsEvent>
        @Suppress("UNCHECKED_CAST")
        get() = model as DefaultListModel<AnalyticsEvent>

    init {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        val renderer = EventCellRenderer()
        fixedCellHeight = renderer.cellHeight
        cellRenderer = renderer
        addListSelectionListener { if (!it.valueIsAdjusting) notifySelection() }
    }

    // The platform look and feel paints the whole selected row; the basic UI leaves that to the renderer.
    override fun updateUI() {
        setUI(BasicListUI())
    }

    override fun getToolTipText(e: MouseEvent): String? {
        val i = locationToIndex(e.point)
        return if (i >= 0 && getCellBounds(i, i)?.contains(e.point) == true) events.getElementAt(i).title else null
    }

    /** Replaces the whole content at once, so a big refilter fires a single update. */
    fun setEvents(items: List<AnalyticsEvent>) {
        val m = DefaultListModel<AnalyticsEvent>()
        m.addAll(items)
        model = m
        if (m.size() > 0) selectedIndex = 0
        notifySelection()
    }

    fun prepend(event: AnalyticsEvent, limit: Int) {
        events.add(0, event)
        if (events.size() > limit) events.removeRange(limit, events.size() - 1)
        if (selectedIndex < 0) selectedIndex = 0
    }

    fun clear() {
        events.clear()
        shown = null
        onSelect(null)
    }

    private fun notifySelection() {
        val v = selectedValue
        if (v != null && v === shown) return
        shown = v
        onSelect(v)
    }
}

/** One reusable component paints every row, so rows share a fixed height and nothing is allocated per cell. */
private class EventCellRenderer : ListCellRenderer<AnalyticsEvent> {
    private val selectedFill: Color = JBUI.CurrentTheme.List.Selection.background(true)
    private var fill: Color = selectedFill
    private var stroke: Color = selectedFill

    private val label = object : JBLabel("X", SwingConstants.CENTER) {
        override fun getPreferredSize() = super.getPreferredSize().apply { width = 0 }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = fill
            g2.fillRoundRect(0, 0, width - 1, height - 1, ARC, ARC)
            g2.color = stroke
            g2.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC)
            g2.dispose()
            super.paintComponent(g)
        }
    }.apply {
        isOpaque = false
        border = JBUI.Borders.empty(11, 9)
    }

    private val cell = JPanel(BorderLayout()).apply {
        isOpaque = true
        border = JBUI.Borders.empty(4, 10)
        add(label, BorderLayout.CENTER)
    }

    val cellHeight: Int get() = cell.preferredSize.height

    override fun getListCellRendererComponent(
        list: JList<out AnalyticsEvent>, value: AnalyticsEvent, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        fill = if (isSelected) selectedFill else JBColor.namedColor("Button.background", list.background)
        stroke = if (isSelected) selectedFill.brighter().brighter() else JBColor.border()
        label.text = value.title
        label.foreground = if (isSelected) list.selectionForeground else list.foreground
        cell.background = list.background
        return cell
    }

    private companion object {
        const val ARC = 8
    }
}
