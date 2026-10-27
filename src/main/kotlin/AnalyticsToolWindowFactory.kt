package com.badcompanydevz

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

data class AnalyticsEvent(val name: String, val params: List<Pair<String, String>>) {
    val title: String get() = params.firstOrNull { it.first == "eventLabel" }?.second?.takeIf { it.isNotBlank() } ?: name
}

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

class LogcatReader(private val onEvent: (AnalyticsEvent) -> Unit, private val onProperty: (String, String) -> Unit) :
    Disposable {
    @Volatile
    private var process: Process? = null

    @Volatile
    private var generation = 0

    private fun adbPath(): String {
        val exe = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
        val roots = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" },
            System.getProperty("user.home")?.let { "$it/Android/Sdk" },
            System.getProperty("user.home")?.let { "$it/Library/Android/sdk" }
        )
        return roots.map { java.io.File(it, "platform-tools/$exe") }
            .firstOrNull { it.isFile }?.absolutePath ?: "adb"
    }

    private fun adb(vararg args: String) =
        ProcessBuilder(listOf(adbPath()) + args).redirectErrorStream(true).start()

    private fun adbSetprop(vararg args: String) {
        val p = adb(*args)
        try {
            if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly()
        } finally {
            p.destroyForcibly()
        }
    }

    @Synchronized
    fun start() {
        stop()
        val gen = ++generation
        Thread {
            try {
                adbSetprop("shell", "setprop", "log.tag.FA", "VERBOSE")
                adbSetprop("shell", "setprop", "log.tag.FA-SVC", "VERBOSE")
                if (gen != generation) return@Thread
                val p = adb("logcat", "-v", "time", "-s", "FA", "FA-SVC")
                if (gen != generation) { p.destroyForcibly(); return@Thread }
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
                    com.intellij.openapi.diagnostic.Logger.getInstance(LogcatReader::class.java)
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

class AnalyticsToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = AnalyticsPanel()
        Disposer.register(toolWindow.disposable, panel.reader)
        panel.reader.start()
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}

class AnalyticsPanel : JPanel(BorderLayout()) {
    private val model = DefaultListModel<AnalyticsEvent>()
    private val list = JBList(model)
    private val paramsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val propsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val properties = linkedMapOf<String, String>()

    val reader = LogcatReader(
        onEvent = { e ->
            invoke {
                model.add(0, e)
                if (model.size() > MAX_EVENTS) model.removeRange(MAX_EVENTS, model.size() - 1)
                if (list.selectedIndex < 0) list.selectedIndex = 0
            }
        },
        onProperty = { k, v -> invoke { properties[k] = v; renderRows(propsPanel, properties.toList()) } }
    )

    private companion object {
        const val MAX_EVENTS = 1000
    }

    private fun invoke(block: () -> Unit) = ApplicationManager.getApplication().invokeLater(block)

    init {
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = ListCellRenderer<AnalyticsEvent> { l: JList<out AnalyticsEvent>, v, _, sel, _ ->
            val label = JBLabel(v.title, SwingConstants.CENTER).apply {
                isOpaque = true
                border = JBUI.Borders.compound(
                    JBUI.Borders.customLine(JBColor.border(), 1), JBUI.Borders.empty(10, 8)
                )
                background = if (sel) l.selectionBackground else l.background
                foreground = if (sel) l.selectionForeground else l.foreground
            }
            JPanel(BorderLayout()).apply {
                isOpaque = false
                border = JBUI.Borders.empty(4, 10)
                add(label, BorderLayout.CENTER)
            }
        }
        list.addListSelectionListener {
            renderRows(paramsPanel, list.selectedValue?.params.orEmpty())
        }

        val cards = CardLayout()
        val body = JPanel(cards).apply {
            add(JBScrollPane(wrap(paramsPanel)).apply { border = JBUI.Borders.empty() }, "Parameters")
            add(JBScrollPane(wrap(propsPanel)).apply { border = JBUI.Borders.empty() }, "Properties")
        }
        val header = JPanel(GridLayout(1, 2))
        val tabButtons = listOf("Parameters", "Properties").map { name ->
            JBLabel(name, SwingConstants.CENTER).apply {
                isOpaque = true
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }
        }
        fun select(i: Int) {
            cards.show(body, if (i == 0) "Parameters" else "Properties")
            tabButtons.forEachIndexed { idx, b ->
                b.background = if (idx == i) JBColor.border() else list.background
                b.border = JBUI.Borders.compound(
                    JBUI.Borders.customLine(if (idx == i) JBColor.foreground() else list.background, 0, 0, 2, 0),
                    JBUI.Borders.empty(8)
                )
            }
        }
        tabButtons.forEachIndexed { i, b ->
            b.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = select(i)
            })
            header.add(b)
        }
        select(0)
        val tabs = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 6, 0, 6)
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
        val clear = JButton("Clear").apply {
            addActionListener {
                this@AnalyticsPanel.model.clear(); this@AnalyticsPanel.properties.clear()
                renderRows(paramsPanel, emptyList()); renderRows(propsPanel, emptyList())
            }
        }
        val sync = JButton("Sync").apply {
            toolTipText = "Reconnect to adb and re-enable Firebase Analytics debug logging"
            addActionListener { reader.start() }
        }
        val left = JPanel(BorderLayout()).apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply { add(clear); add(sync) }, BorderLayout.NORTH)
            add(JBScrollPane(list), BorderLayout.CENTER)
        }
        add(
            JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs).apply { dividerLocation = 280; border = JBUI.Borders.empty() },
            BorderLayout.CENTER
        )
    }

    private fun wrap(p: JPanel) = JPanel(BorderLayout()).apply { add(p, BorderLayout.NORTH) }

    private fun renderRows(target: JPanel, rows: List<Pair<String, String>>) {
        target.removeAll()
        rows.forEach { (k, v) ->
            target.add(JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(6, 8)
                alignmentX = Component.LEFT_ALIGNMENT
                add(JBLabel(k), BorderLayout.NORTH)
                add(JBLabel(v.ifEmpty { "-" }, SwingConstants.RIGHT), BorderLayout.CENTER)
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, preferredSize.height)
            })
        }
        target.revalidate(); target.repaint()
    }
}
