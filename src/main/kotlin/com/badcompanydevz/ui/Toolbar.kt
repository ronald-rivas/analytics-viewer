package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Status indicator on the left; Sync and Clear icons on the right, 8dp apart. */
class Toolbar(status: JComponent, onSync: () -> Unit, onClear: () -> Unit) : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(status)
        add(Box.createHorizontalGlue())
        add(IconButton(AllIcons.Actions.Refresh, Strings["action_sync"], onSync))
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(IconButton(AllIcons.Actions.GC, Strings["action_clear"], onClear))
    }
}
