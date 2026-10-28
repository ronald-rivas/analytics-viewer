package com.badcompanydevz.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon

/** Borderless icon that shows a rounded hover background and runs [action] on click. */
class IconButton(icon: Icon, tip: String, action: () -> Unit) : JBLabel(icon) {
    private var hover = false

    init {
        toolTipText = tip
        isOpaque = false
        border = JBUI.Borders.empty(3)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = action()
            override fun mouseEntered(e: MouseEvent) {
                hover = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                hover = false
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        if (hover) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
            val arc = JBUI.scale(6)
            g2.fillRoundRect(0, 0, width, height, arc, arc)
            g2.dispose()
        }
        super.paintComponent(g)
    }
}
