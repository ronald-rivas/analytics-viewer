package com.badcompanydevz.ui

import com.badcompanydevz.model.AnalyticsEvent

/** Applies parameter visibility and value matching independently of the event-name search. */
internal class ParameterFilter(private val values: Map<String, String?>, private val search: String) {
    private val selected = values.filterValues { it != null }

    fun visibleParams(event: AnalyticsEvent?): List<Pair<String, String>> {
        val params = event?.params.orEmpty()
        if (values.isEmpty()) return params
        return params.filter { (name, _) -> name in selected }
    }

    fun matches(event: AnalyticsEvent): Boolean {
        if (values.isNotEmpty() && selected.isEmpty()) return false
        if (search.isNotBlank()) return event.title.contains(search, true) || event.name.contains(search, true)
        return (selected.isEmpty() || event.params.any { (name, actual) ->
            selected[name]?.let { it.isBlank() || actual.contains(it, true) } == true
        }) && selected.all { (name, value) ->
            value.isNullOrBlank() || event.params.any { (key, actual) -> key == name && actual.contains(value, true) }
        }
    }
}