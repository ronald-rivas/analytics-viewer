package com.badcompanydevz.data

import java.io.File
import java.util.concurrent.TimeUnit

object Adb {
    private fun path(): String {
        val exe = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
        val roots = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" },
            System.getProperty("user.home")?.let { "$it/Android/Sdk" },
            System.getProperty("user.home")?.let { "$it/Library/Android/sdk" }
        )
        return roots.map { File(it, "platform-tools/$exe") }
            .firstOrNull { it.isFile }?.absolutePath ?: "adb"
    }

    fun start(vararg args: String): Process =
        ProcessBuilder(listOf(path()) + args).redirectErrorStream(true).start()

    fun runAndWait(vararg args: String) {
        val p = start(*args)
        try {
            if (!p.waitFor(10, TimeUnit.SECONDS)) p.destroyForcibly()
        } finally {
            p.destroyForcibly()
        }
    }

    fun isDeviceConnected(): Boolean = try {
        val p = start("get-state")
        try {
            val done = p.waitFor(3, TimeUnit.SECONDS)
            done && p.exitValue() == 0 && p.inputStream.bufferedReader().readText().trim() == "device"
        } finally {
            p.destroyForcibly()
        }
    } catch (e: Exception) {
        false
    }
}
