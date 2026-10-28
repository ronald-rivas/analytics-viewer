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
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

object Strings {
    private val values: Map<String, String> by lazy {
        val stream = Strings::class.java.getResourceAsStream("/strings.xml") ?: return@lazy emptyMap()
        stream.use {
            val nodes = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(it).getElementsByTagName("string")
            (0 until nodes.length).associate { i ->
                val n = nodes.item(i)
                n.attributes.getNamedItem("name").nodeValue to n.textContent
            }
        }
    }

    operator fun get(key: String) = values[key] ?: key
}

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
    private var model = DefaultListModel<AnalyticsEvent>()
    private val list = object : JBList<AnalyticsEvent>(model) {
        override fun updateUI() { setUI(javax.swing.plaf.basic.BasicListUI()) }
        override fun getToolTipText(e: MouseEvent): String? {
            val i = locationToIndex(e.point)
            return if (i >= 0 && getCellBounds(i, i)?.contains(e.point) == true) model.getElementAt(i).title else null
        }
    }
    private val paramsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val propsPanel = JPanel().apply { layout = BoxLayout(this, BoxLayout.Y_AXIS) }
    private val properties = linkedMapOf<String, String>()
    private val events = ArrayList<AnalyticsEvent>()
    private var filter = ""

    private fun matches(e: AnalyticsEvent) =
        filter.isBlank() || e.title.contains(filter, true) || e.name.contains(filter, true)

    private var shownEvent: AnalyticsEvent? = null

    private fun showSelected() {
        val v = list.selectedValue
        if (v === shownEvent && v != null) return
        shownEvent = v
        renderRows(paramsPanel, v?.params.orEmpty())
    }

    private fun refilter() {
        val m = DefaultListModel<AnalyticsEvent>()
        m.addAll(events.filter { matches(it) })
        model = m
        list.model = m
        if (m.size() > 0) list.selectedIndex = 0
        showSelected()
    }

    val reader = LogcatReader(
        onEvent = { e ->
            invoke {
                events.add(0, e)
                if (events.size > MAX_EVENTS) events.subList(MAX_EVENTS, events.size).clear()
                if (matches(e)) {
                    model.add(0, e)
                    if (model.size() > MAX_EVENTS) model.removeRange(MAX_EVENTS, model.size() - 1)
                    if (list.selectedIndex < 0) list.selectedIndex = 0
                }
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
        val selFill = JBUI.CurrentTheme.List.Selection.background(true)
        list.selectionBackground = java.awt.Color(0, 0, 0, 0)
        var fill: java.awt.Color = selFill
        var stroke: java.awt.Color = selFill
        val label = object : JBLabel("", SwingConstants.CENTER) {
                override fun getPreferredSize() = super.getPreferredSize().apply { width = 0 }
                override fun paintComponent(g: java.awt.Graphics) {
                    val g2 = g.create() as java.awt.Graphics2D
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                    g2.color = fill
                    g2.fillRoundRect(0, 0, width - 1, height - 1, 8, 8)
                    g2.color = stroke
                    g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8)
                    g2.dispose()
                    super.paintComponent(g)
                }
            }.apply {
                isOpaque = false
                border = JBUI.Borders.empty(11, 9)
            }
        val cell = JPanel(BorderLayout()).apply {
            isOpaque = true
            border = JBUI.Borders.empty(4, 10)
            add(label, BorderLayout.CENTER)
        }
        label.text = "X"
        list.fixedCellHeight = cell.preferredSize.height
        list.cellRenderer = ListCellRenderer<AnalyticsEvent> { l: JList<out AnalyticsEvent>, v, _, sel, _ ->
            fill = if (sel) selFill else JBColor.namedColor("Button.background", l.background)
            stroke = if (sel) selFill.brighter().brighter() else JBColor.border()
            label.text = v.title
            label.foreground = if (sel) l.selectionForeground else l.foreground
            cell.background = l.background
            cell
        }
        list.addListSelectionListener { if (!it.valueIsAdjusting) showSelected() }

        val tabLine = JBColor.border()
        val cards = CardLayout()
        val body = JPanel(cards).apply {
            border = object : javax.swing.border.Border {
                override fun getBorderInsets(c: Component) = JBUI.insets(0, 1, 0, 1)
                override fun isBorderOpaque() = false
                override fun paintBorder(c: Component, g: java.awt.Graphics, x: Int, y: Int, w: Int, h: Int) {
                    val g2 = g.create() as java.awt.Graphics2D
                    val clear = java.awt.Color(tabLine.red, tabLine.green, tabLine.blue, 0)
                    g2.paint = java.awt.GradientPaint(0f, y.toFloat(), tabLine, 0f, (y + h).toFloat(), clear)
                    g2.fillRect(x, y, 1, h)
                    g2.fillRect(x + w - 1, y, 1, h)
                    g2.dispose()
                }
            }
            add(JBScrollPane(wrap(paramsPanel)).apply { border = JBUI.Borders.empty() }, "Parameters")
            add(JBScrollPane(wrap(propsPanel)).apply { border = JBUI.Borders.empty() }, "Properties")
        }
        val header = JPanel(GridLayout(1, 2))
        var selectedTab = 0
        val tabButtons = listOf(Strings["tab_parameters"], Strings["tab_properties"]).mapIndexed { idx, name ->
            object : JBLabel(name, SwingConstants.CENTER) {
                override fun paintComponent(g: java.awt.Graphics) {
                    val g2 = g.create() as java.awt.Graphics2D
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                    val arc = JBUI.scale(12)
                    val w = width - 1
                    val h = height + arc
                    if (idx == selectedTab) {
                        g2.color = list.background
                        g2.fillRoundRect(0, 0, w, h, arc, arc)
                        g2.color = tabLine
                        g2.drawRoundRect(0, 0, w, h, arc, arc)
                    } else {
                        g2.color = tabLine
                        g2.fillRoundRect(0, 0, w + 1, h, arc, arc)
                        g2.drawLine(0, height - 1, width, height - 1)
                    }
                    g2.dispose()
                    super.paintComponent(g)
                }
            }.apply {
                isOpaque = false
                border = JBUI.Borders.empty(8)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }
        }
        fun select(i: Int) {
            selectedTab = i
            cards.show(body, if (i == 0) "Parameters" else "Properties")
            tabButtons.forEach { it.repaint() }
        }
        tabButtons.forEachIndexed { i, b ->
            b.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = select(i)
            })
            header.add(b)
        }
        select(0)
        val tabs = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 6)
            add(header, BorderLayout.NORTH)
            add(body, BorderLayout.CENTER)
        }
        fun iconButton(icon: javax.swing.Icon, tip: String, action: () -> Unit): JBLabel {
            var hover = false
            return object : JBLabel(icon) {
                override fun paintComponent(g: java.awt.Graphics) {
                    if (hover) {
                        val g2 = g.create() as java.awt.Graphics2D
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
                        g2.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
                        val arc = JBUI.scale(6)
                        g2.fillRoundRect(0, 0, width, height, arc, arc)
                        g2.dispose()
                    }
                    super.paintComponent(g)
                }
            }.apply {
                toolTipText = tip
                isOpaque = false
                border = JBUI.Borders.empty(3)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) = action()
                    override fun mouseEntered(e: MouseEvent) { hover = true; repaint() }
                    override fun mouseExited(e: MouseEvent) { hover = false; repaint() }
                })
            }
        }
        val clear = iconButton(com.intellij.icons.AllIcons.Actions.GC, Strings["action_clear"]) {
            events.clear(); model.clear()
            renderRows(paramsPanel, emptyList())
        }
        val sync = iconButton(
            com.intellij.icons.AllIcons.Actions.Refresh,
            Strings["action_sync"]
        ) { reader.start() }
        val searchField = com.intellij.ui.components.JBTextField().apply {
            emptyText.text = Strings["filter_hint"]
            val debounce = javax.swing.Timer(150) { filter = text.trim(); refilter() }.apply { isRepeats = false }
            document.addDocumentListener(object : javax.swing.event.DocumentListener {
                private fun changed() = debounce.restart()
                override fun insertUpdate(e: javax.swing.event.DocumentEvent) = changed()
                override fun removeUpdate(e: javax.swing.event.DocumentEvent) = changed()
                override fun changedUpdate(e: javax.swing.event.DocumentEvent) = changed()
            })
        }
        val left = JPanel(BorderLayout()).apply {
            add(JPanel(BorderLayout(0, 6)).apply {
                border = JBUI.Borders.empty(4, 10)
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.X_AXIS)
                    add(javax.swing.Box.createHorizontalGlue())
                    add(sync)
                    add(javax.swing.Box.createRigidArea(java.awt.Dimension(JBUI.scale(8), 0)))
                    add(clear)
                }, BorderLayout.NORTH)
                add(searchField, BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            add(
                JBScrollPane(list, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER).apply {
                    border = JBUI.Borders.empty()
                    viewportBorder = null
                },
                BorderLayout.CENTER
            )
        }
        add(
            JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs).apply {
                setUI(object : javax.swing.plaf.basic.BasicSplitPaneUI() {
                    override fun createDefaultDivider() = object : javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                        override fun paint(g: java.awt.Graphics) {
                            g.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
                            val line = JBUI.scale(1)
                            g.fillRect((width - line) / 2, 0, line, height)
                        }
                    }.apply { border = null }
                })
                dividerSize = JBUI.scale(5)
                dividerLocation = 280
                border = JBUI.Borders.empty()
            },
            BorderLayout.CENTER
        )
    }

    private fun wrap(p: JPanel) = JPanel(BorderLayout()).apply { add(p, BorderLayout.NORTH) }

    private fun renderRows(target: JPanel, rows: List<Pair<String, String>>) {
        target.removeAll()
        rows.forEach { (k, v) ->
            target.add(JPanel(BorderLayout(0, JBUI.scale(4))).apply {
                border = JBUI.Borders.empty(6, 8)
                alignmentX = LEFT_ALIGNMENT
                add(JBLabel(k), BorderLayout.NORTH)
                add(JBLabel(v.ifEmpty { "-" }, SwingConstants.RIGHT), BorderLayout.CENTER)
                maximumSize = java.awt.Dimension(Int.MAX_VALUE, preferredSize.height)
            })
        }
        target.revalidate(); target.repaint()
    }
}
