package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

/** Status indicator on the left; Run/Pause, Sync and Clear icons on the right, 8dp apart. */
class Toolbar(status: JComponent, onPause: () -> Unit, onSync: () -> Unit, onClear: () -> Unit, onSave: () -> Unit) : JPanel() {
    val pauseButton = IconButton(runIcon, Strings["action_run"], onPause)
    val syncButton = IconButton(AllIcons.Actions.Refresh, Strings["action_sync"], onSync)
    val saveButton = IconButton(AllIcons.Actions.Download, Strings["action_save"], onSave)
    val separator: Component = add(JSeparator(SwingConstants.VERTICAL).apply {
        foreground = JBColor.border()
        maximumSize = Dimension(JBUI.scale(2), JBUI.scale(16))
    })

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(status)
        add(Box.createHorizontalGlue())
        add(pauseButton)
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(syncButton)
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(IconButton(AllIcons.Actions.GC, Strings["action_clear"], onClear))
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(separator)
        add(Box.createRigidArea(Dimension(JBUI.scale(8), 0)))
        add(saveButton)
    }

    companion object {
        val runIcon = AllIcons.Toolwindows.ToolWindowRun
    }
}
