package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Status indicator on the left; Pause, Sync and Clear icons on the right, 8dp apart. */
class Toolbar(status: JComponent, onPause: () -> Unit, onSync: () -> Unit, onClear: () -> Unit) : JPanel() {
    val pauseButton = IconButton(AllIcons.Actions.Pause, Strings["action_pause"], onPause)

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(status)
        add(Box.createHorizontalGlue())
        add(pauseButton)
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(IconButton(AllIcons.Actions.Refresh, Strings["action_sync"], onSync))
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(IconButton(AllIcons.Actions.GC, Strings["action_clear"], onClear))
    }
}
