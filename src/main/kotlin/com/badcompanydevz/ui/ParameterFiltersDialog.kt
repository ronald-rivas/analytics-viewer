package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Edits a snapshot of the parameter filters; cancelling leaves the applied filters unchanged. */
class ParameterFiltersDialog(names: Collection<String>, current: Map<String, String?>) : DialogWrapper(true) {
    private val controls = names.sorted().associateWith { name ->
        JBCheckBox(name, current[name] != null || !current.containsKey(name)) to JBTextField().apply {
            emptyText.text = Strings["parameter_filter_hint"]
            text = current[name].orEmpty()
        }
    }

    val filters: Map<String, String?>
        get() = controls.mapValues { (_, control) ->
            if (control.first.isSelected) control.second.text.trim() else null
        }

    init {
        title = Strings["parameter_filters_title"]
        setOKButtonText(Strings["action_apply"])
        init()
    }

    override fun createCenterPanel(): JComponent {
        val rows = JPanel(GridLayout(0, 2, JBUI.scale(16), JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(8)
            controls.values.forEach { (check, field) ->
                add(check)
                add(field)
            }
        }
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(rows), BorderLayout.CENTER)
            preferredSize = Dimension(JBUI.scale(450), JBUI.scale(300))
        }
    }
}