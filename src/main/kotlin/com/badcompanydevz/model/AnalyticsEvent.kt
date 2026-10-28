package com.badcompanydevz.model

data class AnalyticsEvent(val name: String, val params: List<Pair<String, String>>) {
    val title: String
        get() = params.firstOrNull { it.first == "eventLabel" }?.second?.takeIf { it.isNotBlank() } ?: name
}
