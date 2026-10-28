package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.Border

/** Two folder-style tabs (Parameters / Properties) joined to a content box with fading side lines. */
class FolderTabs(parameters: JComponent, properties: JComponent) : JPanel(BorderLayout()) {
    private val lineColor = JBColor.border()
    private val cards = CardLayout()
    private val body = JPanel(cards)
    private var selected = 0
    private val tabs = mutableListOf<JBLabel>()

    init {
        border = JBUI.Borders.empty(0, 6)
        body.border = FadingSideBorder(lineColor)
        body.add(scrollable(parameters), PARAMETERS)
        body.add(scrollable(properties), PROPERTIES)

        val header = JPanel(GridLayout(1, 2))
        listOf(Strings["tab_parameters"], Strings["tab_properties"]).forEachIndexed { i, name -> tabs += tab(name, i) }
        tabs.forEach { header.add(it) }
        select(0)

        add(header, BorderLayout.NORTH)
        add(body, BorderLayout.CENTER)
    }

    private fun select(index: Int) {
        selected = index
        cards.show(body, if (index == 0) PARAMETERS else PROPERTIES)
        tabs.forEach { it.repaint() }
    }

    private fun scrollable(content: JComponent) =
        JBScrollPane(JPanel(BorderLayout()).apply { add(content, BorderLayout.NORTH) })
            .apply { border = JBUI.Borders.empty() }

    private fun tab(name: String, index: Int) = object : JBLabel(name, SwingConstants.CENTER) {
        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val arc = JBUI.scale(12)
            val w = width - 1
            val h = height + arc
            if (index == selected) {
                g2.color = UIUtil.getListBackground()
                g2.fillRoundRect(0, 0, w, h, arc, arc)
                g2.color = lineColor
                g2.drawRoundRect(0, 0, w, h, arc, arc)
            } else {
                g2.color = lineColor
                g2.fillRoundRect(0, 0, w + 1, h, arc, arc)
                g2.drawLine(0, height - 1, width, height - 1)
            }
            g2.dispose()
            super.paintComponent(g)
        }
    }.apply {
        isOpaque = false
        border = JBUI.Borders.empty(8)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = select(index)
        })
    }

    private companion object {
        const val PARAMETERS = "Parameters"
        const val PROPERTIES = "Properties"
    }
}

/** 1px vertical lines on both sides that fade to 0% opacity towards the bottom. */
private class FadingSideBorder(private val color: java.awt.Color) : Border {
    override fun getBorderInsets(c: Component) = JBUI.insets(0, 1)
    override fun isBorderOpaque() = false

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
        val g2 = g.create() as Graphics2D
        g2.paint = GradientPaint(0f, y.toFloat(), color, 0f, (y + h).toFloat(), ColorUtil.withAlpha(color, 0.0))
        g2.fillRect(x, y, 1, h)
        g2.fillRect(x + w - 1, y, 1, h)
        g2.dispose()
    }
}
