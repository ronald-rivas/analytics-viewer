package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


/**
 * A dialog for saving current filters/data, following the design of frame-save.png.
 */
class SavedAnalyticsDialog(val path: String) : DialogWrapper(true) {

    private val filteredCheckbox = JCheckBox("Filtered").apply {
        font = font.deriveFont(14f)
        isSelected = true
    }

    val isFiltered: Boolean
        get() = filteredCheckbox.isSelected

    init {
        title = Strings["action_save"]
        setOKButtonText(Strings["action_save"])
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(24)
        }

        val textLabel = JLabel("You are about to generate and save an Excel file to the following path: $path")
        mainPanel.add(textLabel, BorderLayout.CENTER)

        return mainPanel
    }

    override fun createSouthPanel(): JComponent {
        val southPanel = super.createSouthPanel()

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(0, 12, 12, 12)

            val leftWrapper = JPanel().apply {
                isOpaque = false
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(Box.createVerticalGlue())
                add(filteredCheckbox)
                add(Box.createVerticalGlue())
            }

            add(leftWrapper, BorderLayout.WEST)
            add(southPanel, BorderLayout.EAST)
        }
    }

    override fun doOKAction() {
        super.doOKAction()
    }
}