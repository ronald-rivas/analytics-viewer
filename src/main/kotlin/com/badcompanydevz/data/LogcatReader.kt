package com.badcompanydevz.data

import com.badcompanydevz.model.AnalyticsEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger

class LogcatReader(
    private val onEvent: (AnalyticsEvent) -> Unit,
    private val onProperty: (String, String) -> Unit
) : Disposable {
    @Volatile
    private var process: Process? = null

    @Volatile
    private var generation = 0

    @Synchronized
    fun start() {
        stop()
        val gen = ++generation
        Thread {
            try {
                Adb.runAndWait("shell", "setprop", "log.tag.FA", "VERBOSE")
                Adb.runAndWait("shell", "setprop", "log.tag.FA-SVC", "VERBOSE")
                if (gen != generation) return@Thread
                val p = Adb.start("logcat", "-v", "time", "-s", "FA", "FA-SVC")
                if (gen != generation) {
                    p.destroyForcibly()
                    return@Thread
                }
                process = p
                p.inputStream.bufferedReader().useLines { lines ->
                    for (raw in lines) {
                        if (gen != generation) break
                        val line = raw.take(MAX_LINE)
                        AnalyticsParser.parseEvent(line)?.let { onEvent(it) }
                        AnalyticsParser.parseProperty(line)?.let { onProperty(it.first, it.second) }
                    }
                }
            } catch (e: Exception) {
                if (gen == generation) {
                    Logger.getInstance(LogcatReader::class.java)
                        .warn("Logcat reader failed: ${e.javaClass.simpleName}")
                }
            }
        }.apply { isDaemon = true; name = "AnalyticsViewer-logcat" }.start()
    }

    @Synchronized
    private fun stop() {
        generation++
        process?.destroyForcibly()
        process = null
    }

    override fun dispose() = stop()

    private companion object {
        const val MAX_LINE = 8192
    }
}
