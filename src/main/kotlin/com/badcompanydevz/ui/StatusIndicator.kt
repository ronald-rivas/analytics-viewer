package com.badcompanydevz.ui

import com.badcompanydevz.Strings
import com.badcompanydevz.data.Adb
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.concurrent.TimeUnit

/**
 * Dot that shows whether an adb device is ready (green), missing (red) or not checked yet (gray).
 * Polls adb off the UI thread and calls [onReconnect] when the state goes from red to green.
 */
class StatusIndicator(private val onReconnect: () -> Unit) : JBLabel(), Disposable {
    private var connected: Boolean? = null

    private val poll = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
        val now = Adb.isDeviceConnected()
        ApplicationManager.getApplication().invokeLater { update(now) }
    }, 0, POLL_SECONDS, TimeUnit.SECONDS)

    private fun update(now: Boolean) {
        val prev = connected
        if (prev == now) return
        connected = now
        toolTipText = Strings[if (now) "status_connected" else "status_disconnected"]
        repaint()
        if (prev == false && now) onReconnect()
    }

    override fun getPreferredSize() = Dimension(JBUI.scale(14), JBUI.scale(22))
    override fun getMinimumSize() = preferredSize
    override fun getMaximumSize() = preferredSize

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = when (connected) {
            true -> CONNECTED
            false -> DISCONNECTED
            null -> JBColor.GRAY
        }
        val d = JBUI.scale(8)
        g2.fillOval((width - d) / 2, (height - d) / 2, d, d)
        g2.dispose()
    }

    override fun dispose() {
        poll.cancel(true)
    }

    private companion object {
        const val POLL_SECONDS = 2L
        val CONNECTED = JBColor(0x3FB950, 0x3FB950)
        val DISCONNECTED = JBColor(0xE5534B, 0xE5534B)
    }
}
