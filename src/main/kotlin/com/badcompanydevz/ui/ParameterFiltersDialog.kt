package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ThreeStateCheckBox
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextAttribute
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager

/** Edits a snapshot of the parameter filters; cancelling leaves the applied filters unchanged. */
class ParameterFiltersDialog(names: Collection<String>, current: Map<String, String?>) : DialogWrapper(true) {
    private val controls = names.sorted().associateWith { name ->
        JBCheckBox(name, current[name] != null || !current.containsKey(name)) to JBTextField().apply {
            emptyText.text = Strings["parameter_filter_hint"]
            text = current[name].orEmpty()
        }
    }
    private val allCheck = object : ThreeStateCheckBox(Strings["parameter_filter_all"]) {
        override fun nextState(): State =
            if (state == State.NOT_SELECTED) State.SELECTED else State.NOT_SELECTED
    }

    val filters: Map<String, String?>
        get() = controls.mapValues { (_, control) ->
            if (control.first.isSelected) control.second.text.trim() else null
        }

    init {
        controls.values.forEach { (check, _) -> check.addActionListener { updateAllCheck() } }
        allCheck.addActionListener {
            val select = allCheck.state == ThreeStateCheckBox.State.SELECTED
            controls.values.forEach { (check, _) -> check.isSelected = select }
            updateAllCheck()
        }
        updateAllCheck()
        title = Strings["parameter_filters_title"]
        setOKButtonText(Strings["action_apply"])
        init()
    }

    private fun updateAllCheck() {
        val selected = controls.values.count { (check, _) -> check.isSelected }
        allCheck.state = when {
            selected == 0 -> ThreeStateCheckBox.State.NOT_SELECTED
            selected == controls.size -> ThreeStateCheckBox.State.SELECTED
            else -> ThreeStateCheckBox.State.DONT_CARE
        }
    }

    override fun createCenterPanel(): JComponent {
        val rows = JPanel(GridLayout(0, 2, JBUI.scale(16), JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(8)
            add(allCheck)
            add(JPanel())
            controls.values.forEach { (check, field) ->
                add(check)
                add(field)
            }
        }
        val removeFilters = JLabel(Strings["action_remove_filters"]).apply {
            font = font.deriveFont(font.attributes + (TextAttribute.UNDERLINE to TextAttribute.UNDERLINE_ON))
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            val normalColor = foreground
            val hoverColor = UIManager.getColor("Link.activeForeground") ?: Color(0x3574B0)
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) { foreground = hoverColor }
                override fun mouseExited(e: MouseEvent?) { foreground = normalColor }
                override fun mouseClicked(e: MouseEvent?) {
                    controls.values.forEach { (_, field) -> field.text = "" }
                }
            })
        }
        val footer = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 8, 8, 8)
            add(removeFilters, BorderLayout.EAST)
        }
        val scrollContent = JPanel(BorderLayout()).apply {
            add(rows, BorderLayout.NORTH)
        }
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(scrollContent), BorderLayout.CENTER)
            add(footer, BorderLayout.SOUTH)
            preferredSize = Dimension(JBUI.scale(450), JBUI.scale(300))
        }
    }
}