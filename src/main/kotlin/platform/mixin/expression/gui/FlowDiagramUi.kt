/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.expression.gui

import com.demonwav.mcdev.asset.MCDevBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.mxgraph.model.mxCell
import com.mxgraph.swing.mxGraphComponent
import com.mxgraph.util.mxEvent
import com.mxgraph.view.mxGraph
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.SortedMap
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JToolBar
import javax.swing.event.DocumentEvent

class FlowDiagramUi(
    private val graph: mxGraph,
    private val calculateBounds: () -> Dimension,
    private val lineNumberNodes: SortedMap<Int, mxCell>,
) : JPanel(BorderLayout()) {
    private val comp = mxGraphComponent(graph)
    val viewToolbar = ViewToolbar()
    val matchToolbar = MatchToolbar()

    private val toolbars = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(viewToolbar)
        add(matchToolbar)
    }

    init {
        configureGraphComponent()

        add(toolbars, BorderLayout.NORTH)
        add(comp, BorderLayout.CENTER)

        fixBounds()
    }

    fun scrollToLine(lineNumber: Int) {
        lineNumberNodes.tailMap(lineNumber).firstEntry()?.let { (_, node) ->
            scrollCellToVisible(comp, node)
        }
    }

    fun setMatchToolbarVisible(visible: Boolean) {
        matchToolbar.isVisible = visible
    }

    fun refresh() {
        comp.refresh()
        fixBounds()
    }

    fun showExpr(text: String, highlightRoot: FlowNode?) {
        matchToolbar.setExprText("<html>" + makeExprString(text, highlightRoot) + "</html>")
        matchToolbar.isVisible = true
    }

    fun scrollToNode(node: FlowNode) {
        val cell = comp.graph.getChildVertices(comp.graph.defaultParent).asSequence()
            .map { it as mxCell }
            .find { it.value === node }
            ?: return
        comp.scrollCellToVisible(cell, true)
    }

    fun highlightCells(text: String) {
        graph.update {
            val vertices = graph.getChildVertices(graph.defaultParent)
            var scrolled = false

            for (cell in vertices) {
                val flow = (cell as mxCell).value as? FlowNode ?: continue
                val texts = listOf(
                    flow.shortText,
                    flow.longText,
                )

                if (text.isNotEmpty() && texts.any { text in it.lowercase() }) {
                    flow.searchHighlight = true
                    if (!scrolled) {
                        comp.scrollCellToVisible(cell, true)
                        comp.zoomTo(1.2, true)
                        graph.selectionCell = cell
                        scrolled = true
                    }
                } else {
                    flow.searchHighlight = false
                }
            }
        }
        refresh()
    }

    fun onNodeSelected(action: (node: FlowNode?, soft: Boolean) -> Unit) {
        fun highlight(e: MouseEvent, soft: Boolean) {
            val node = (comp.getCellAt(e.x, e.y) as mxCell?)?.value as? FlowNode
            action(node, soft)
            comp.refresh()
            e.consume()
        }

        comp.graphControl.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                highlight(e, false)
            }
        })
        comp.graphControl.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                highlight(e, true)
            }
        })
    }

    private fun configureGraphComponent() {
        graph.view.addListener(mxEvent.SCALE_AND_TRANSLATE) { _, _ ->
            fixBounds()
        }

        graph.isCellsSelectable = false
        graph.isCellsEditable = false
        comp.isConnectable = false
        comp.isPanning = true
        comp.setToolTips(true)
        comp.viewport.setOpaque(true)
        comp.viewport.setBackground(EditorColorsManager.getInstance().globalScheme.defaultBackground)

        comp.graphControl.setOpaque(false)
        comp.verticalScrollBar.setUnitIncrement(16)
        comp.horizontalScrollBar.setUnitIncrement(16)
    }

    private fun fixBounds() {
        comp.graphControl.preferredSize = calculateBounds()
        comp.graphControl.revalidate()
        repaint()
    }

    private fun scrollCellToVisible(comp: mxGraphComponent, node: mxCell) {
        // Scrolls the cell to the top of the screen if possible
        val graph = comp.graph
        val state = graph.view.getState(node) ?: return
        val cellBounds = state.rectangle
        val viewRect = comp.viewport.viewRect
        val targetRect = Rectangle(
            cellBounds.x, cellBounds.y,
            1, viewRect.height
        )
        comp.graphControl.scrollRectToVisible(targetRect)
    }

    inner class ViewToolbar : JToolBar() {
        private val zoomInButton = JButton("+").apply {
            toolTipText = MCDevBundle("mixin.flow_diagram.zoom_in")
            addActionListener {
                comp.zoomIn()
            }
        }
        private val zoomOutButton = JButton("−").apply {
            toolTipText = MCDevBundle("mixin.flow_diagram.zoom_out")
            addActionListener {
                comp.zoomOut()
            }
        }
        private val searchField = JTextField()

        init {
            isFloatable = false
            add(zoomInButton)
            add(zoomOutButton)
            addSeparator(Dimension(20, 0))
            add(JLabel(MCDevBundle("mixin.flow_diagram.search")))
            add(searchField)
        }

        fun onSearchFieldChanged(action: (String) -> Unit) {
            searchField.document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    action(searchField.text)
                }
            })
        }
    }

    inner class MatchToolbar : JToolBar() {
        private val helpLabel = JLabel(MCDevBundle("mixin.flow_diagram.showing_matches")).apply {
            border = BorderFactory.createEmptyBorder(0, 6, 0, 0)
        }

        private val exprText = JLabel(" ").apply {
            font = DiagramStyles.CURRENT_EDITOR_FONT
            border = BorderFactory.createEmptyBorder(0, 15, 0, 5)
        }

        private val refreshButton = makeButton(AllIcons.Actions.Refresh, MCDevBundle("mixin.flow_diagram.rematch"))
        private val clearButton = makeButton(
            AllIcons.Actions.CloseDarkGrey,
            MCDevBundle("mixin.flow_diagram.clear_match"),
        )

        private val buttonPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.RIGHT, 3, 3)
            isOpaque = false
            add(refreshButton)
            add(clearButton)
        }

        init {
            isVisible = false
            isFloatable = false
            layout = BorderLayout()
            add(helpLabel, BorderLayout.WEST)
            add(exprText, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.EAST)
        }

        fun setExprText(text: String) {
            exprText.text = text
            exprText.toolTipText = text
        }

        fun onTextClicked(action: () -> Unit) {
            matchToolbar.exprText.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        action()
                    }
                }
            })
        }

        fun onRefresh(action: () -> Unit) {
            refreshButton.addActionListener {
                action()
            }
        }

        fun onClear(action: () -> Unit) {
            clearButton.addActionListener {
                action()
            }
        }
    }
}

private fun makeButton(icon: Icon, tooltip: String): JButton =
    JButton(icon).apply {
        toolTipText = tooltip
        preferredSize = Dimension(32, 32)
    }

private sealed class HighlightChange : Comparable<HighlightChange> {
    abstract val pos: Int

    data class Start(override val pos: Int, val length: Int, val status: FlowMatchStatus) : HighlightChange()
    data class End(override val pos: Int) : HighlightChange()

    override fun compareTo(other: HighlightChange): Int =
        compareValuesBy(
            this, other,
            { it.pos },
            { if (it is Start) 1 else -1 },
            { -((it as? Start)?.length ?: 0) },
        )
}

private fun makeExprString(text: String, highlightRoot: FlowNode?): String {
    fun escape(str: String) = StringUtil.escapeXmlEntities(StringUtil.escapeStringCharacters(str))

    if (highlightRoot == null) {
        return escape(text)
    }

    val changes = mutableListOf<HighlightChange>()
    for ((status, src) in highlightRoot.matches) {
        if (src == null) {
            continue
        }
        changes.add(HighlightChange.Start(src.startIndex, src.endIndex - src.startIndex, status))
        changes.add(HighlightChange.End(src.endIndex + 1))
    }
    changes.sort()

    val result = StringBuilder()
    var pos = 0
    for (change in changes) {
        result.append(escape(text.substring(pos, change.pos)))
        pos = change.pos
        when (change) {
            is HighlightChange.Start -> {
                result.append("<span style='color: ${change.status.hexColor}'>")
            }
            is HighlightChange.End -> {
                result.append("</span>")
            }
        }
    }
    result.append(escape(text.substring(pos)))

    return result.toString()
}
