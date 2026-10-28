package com.badcompanydevz

import javax.xml.parsers.DocumentBuilderFactory

/** Reads user-visible texts from `/strings.xml` (Android-style resources). */
object Strings {
    private val values: Map<String, String> by lazy {
        val stream = Strings::class.java.getResourceAsStream("/strings.xml") ?: return@lazy emptyMap()
        stream.use {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(it).getElementsByTagName("string")
            (0 until nodes.length).associate { i ->
                val n = nodes.item(i)
                n.attributes.getNamedItem("name").nodeValue to n.textContent
            }
        }
    }

    operator fun get(key: String) = values[key] ?: key
}
