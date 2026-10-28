package com.badcompanydevz.ui

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

/** Vertical list of rows, each with the key on top and its value right-aligned below. */
class KeyValuePanel : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    fun show(rows: List<Pair<String, String>>) {
        removeAll()
        rows.forEach { (k, v) ->
            add(JPanel(BorderLayout(0, JBUI.scale(4))).apply {
                border = JBUI.Borders.empty(6, 8)
                alignmentX = LEFT_ALIGNMENT
                add(JBLabel(k), BorderLayout.NORTH)
                add(JBLabel(v.ifEmpty { "-" }, SwingConstants.RIGHT), BorderLayout.CENTER)
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
            })
        }
        revalidate()
        repaint()
    }
}
