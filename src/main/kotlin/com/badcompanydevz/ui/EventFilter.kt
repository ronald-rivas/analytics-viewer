package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.intellij.ui.components.JBTextField
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/** Always-active text field that reports its trimmed text through [onFilter] after a short debounce. */
class EventFilter(onFilter: (String) -> Unit) : JBTextField() {
    init {
        emptyText.text = Strings["filter_hint"]
        val debounce = Timer(DEBOUNCE_MS) { onFilter(text.trim()) }.apply { isRepeats = false }
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = debounce.restart()
            override fun removeUpdate(e: DocumentEvent) = debounce.restart()
            override fun changedUpdate(e: DocumentEvent) = debounce.restart()
        })
    }

    private companion object {
        const val DEBOUNCE_MS = 150
    }
}
