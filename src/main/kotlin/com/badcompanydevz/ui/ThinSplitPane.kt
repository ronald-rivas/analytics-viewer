package com.badcompanydevz.ui

import com.intellij.util.ui.JBUI
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JSplitPane
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI

/** Horizontal split with a 1dp visible line inside a wider (5dp) grab area. */
class ThinSplitPane(left: JComponent, right: JComponent) : JSplitPane(HORIZONTAL_SPLIT, left, right) {
    init {
        setUI(object : BasicSplitPaneUI() {
            override fun createDefaultDivider() = object : BasicSplitPaneDivider(this) {
                override fun paint(g: Graphics) {
                    g.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
                    val line = JBUI.scale(1)
                    g.fillRect((width - line) / 2, 0, line, height)
                }
            }.apply { border = null }
        })
        dividerSize = JBUI.scale(5)
        dividerLocation = INITIAL_LOCATION
        border = JBUI.Borders.empty()
    }

    private companion object {
        const val INITIAL_LOCATION = 280
    }
}
