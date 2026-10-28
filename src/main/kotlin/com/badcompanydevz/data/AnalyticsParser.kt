package com.badcompanydevz.data

import com.badcompanydevz.model.AnalyticsEvent

object AnalyticsParser {
    private val eventRegex = Regex("""Logging event: origin=([^,]*),name=([^,]*),params=(?:Bundle\[\{(.*)}]|.*)""")
    private val propertyRegex = Regex("""Setting user property: ([^,]+), ?(.*)$""")
    private val splitRegex = Regex(""",\s(?=[^=,\s]+=)""")

    fun parseEvent(line: String): AnalyticsEvent? {
        val m = eventRegex.find(line) ?: return null
        val params = mutableListOf("origin" to m.groupValues[1], "name" to m.groupValues[2])
        m.groupValues[3].takeIf { it.isNotEmpty() }?.split(splitRegex)?.forEach {
            val i = it.indexOf('=')
            if (i > 0) params += it.substring(0, i) to it.substring(i + 1)
        }
        return AnalyticsEvent(m.groupValues[2], params)
    }

    fun parseProperty(line: String): Pair<String, String>? {
        val m = propertyRegex.find(line) ?: return null
        return m.groupValues[1].trim() to m.groupValues[2].trim()
    }
}
