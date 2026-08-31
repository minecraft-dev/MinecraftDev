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

import com.demonwav.mcdev.platform.mixin.expression.MEExpressionMatchUtil
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import com.intellij.psi.SmartPointerManager
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.model.mxCell
import com.mxgraph.util.mxRectangle
import com.mxgraph.view.mxGraph
import java.awt.Dimension
import java.util.SortedMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

private const val OUTER_PADDING = 30.0
private const val INTER_GROUP_SPACING = 75
private const val INTRA_GROUP_SPACING = 75
private const val LINE_NUMBER_STYLE = "LINE_NUMBER"

class FlowDiagram(
    private val scope: CoroutineScope,
    val ui: FlowDiagramUi,
    private val flowGraph: FlowGraph,
    private val clazz: ClassNode,
    val method: MethodNode,
) {
    companion object {
        suspend fun create(
            project: Project,
            scope: CoroutineScope,
            clazz: ClassNode,
            method: MethodNode
        ): FlowDiagram? {
            val flowGraph = FlowGraph.parse(project, clazz, method) ?: return null
            return buildDiagram(scope, flowGraph, clazz, method)
        }
    }

    var matchExpression: ((jump: Boolean) -> Unit) = {}
        private set
    var jumpToExpression: () -> Unit = {}
        private set

    init {
        ui.viewToolbar.onSearchFieldChanged {
            ui.highlightCells(it)
        }

        ui.matchToolbar.onTextClicked {
            jumpToExpression()
        }

        ui.matchToolbar.onRefresh {
            matchExpression(false)
        }

        ui.matchToolbar.onClear {
            clearExpression()
        }

        ui.onNodeSelected { node, soft ->
            flowGraph.highlightMatches(node, soft)
            ui.refresh()
        }

        flowGraph.onHighlightChanged { exprText, node ->
            scope.launch(Dispatchers.EDT) {
                ui.showExpr(exprText, node)
            }
        }
    }

    fun populateMatchStatuses(
        module: Module,
        currentStringLit: PsiElement,
        currentModifierList: PsiModifierList
    ) {
        val stringRef = SmartPointerManager.getInstance(module.project).createSmartPsiElementPointer(currentStringLit)
        val modifierListRef =
            SmartPointerManager.getInstance(module.project).createSmartPsiElementPointer(currentModifierList)
        this.matchExpression = { jump ->
            val oldHighlightRoot = flowGraph.highlightRoot
            ui.setMatchToolbarVisible(false)
            flowGraph.resetMatches()
            scope.launch(Dispatchers.Default) {
                val success = readAction run@{
                    val stringLit = stringRef.element ?: return@run false
                    val modifierList = modifierListRef.element ?: return@run false
                    val expression = stringLit.constantStringValue?.let(MEExpressionMatchUtil::createExpression)
                        ?: return@run false
                    val pool = MEExpressionMatchUtil.createIdentifierPoolFactory(module, clazz, modifierList)(method)
                    for ((virtualInsn, root) in flowGraph.flowMap) {
                        val node = flowGraph.allNodes.getValue(root)
                        MEExpressionMatchUtil.findMatchingInstructions(
                            clazz, method, pool, flowGraph.flowMap, expression, listOf(virtualInsn),
                            ExpressionContext.Type.MODIFY_EXPRESSION_VALUE, // most permissive
                            false,
                            node::reportMatchStatus,
                            node::reportPartialMatch
                        ) {}
                    }
                    flowGraph.setExprText(expression.src.toString())
                    flowGraph.highlightMatches(oldHighlightRoot, false)
                    true
                }
                if (success) {
                    if (jump) {
                        showBestNode()
                    }
                }
                ui.refresh()
            }
        }
        this.jumpToExpression = {
            scope.launch {
                readAction {
                    val target = stringRef.element
                    if (target is Navigatable && target.isValid && target.canNavigate()) {
                        target.navigate(true)
                    }
                }
            }
        }
        matchExpression(true)
    }

    private fun showBestNode() {
        val bestNode = flowGraph.orderedNodes.maxBy { it.matchScore }
        flowGraph.highlightMatches(bestNode, false)
        ui.scrollToNode(bestNode)
    }

    private fun clearExpression() {
        ui.setMatchToolbarVisible(false)
        flowGraph.resetMatches()
        ui.refresh()
        matchExpression = {}
        jumpToExpression = {}
    }
}

private suspend fun buildDiagram(
    scope: CoroutineScope,
    flowGraph: FlowGraph,
    clazz: ClassNode,
    method: MethodNode
): FlowDiagram {
    val graph = MxFlowGraph(flowGraph)
    setupStyles(graph)
    val groupedCells = addGraphContent(graph, flowGraph)
    val lineNumberNodes = sortedMapOf<Int, mxCell>()
    val calculateBounds = layOutGraph(graph, groupedCells, lineNumberNodes)

    val ui = withContext(Dispatchers.EDT) {
        FlowDiagramUi(graph, calculateBounds, lineNumberNodes)
    }
    return FlowDiagram(scope, ui, flowGraph, clazz, method)
}

private class MxFlowGraph(private val flowGraph: FlowGraph) : mxGraph() {
    override fun getToolTipForCell(cell: Any?): String? {
        val flow = (cell as? mxCell)?.value as? FlowNode ?: return super.getToolTipForCell(cell)
        val lines = mutableListOf<String>()
        if (flowGraph.shouldShowTooltips()) {
            flow.currentMatchResult?.let { match ->
                lines += match.toString(
                    prefix = "`<span style='font-family: ${DiagramStyles.CURRENT_EDITOR_FONT};'>",
                    suffix = "</span>`",
                    transform = StringUtil::escapeXmlEntities
                )
            }
        }
        lines += StringUtil.escapeXmlEntities(flow.longText).replace("\n", "<br>")
        return lines.joinToString(
            prefix = "<html>",
            separator = "<br><br>",
            postfix = "</html>"
        )
    }

    override fun convertValueToString(cell: Any?): String {
        val flow = (cell as? mxCell)?.value as? FlowNode ?: return super.convertValueToString(cell)
        return flow.shortText
    }

    override fun getCellStyle(cell: Any?): MutableMap<String, Any> {
        val result = super.getCellStyle(cell).toMutableMap()
        val flow = (cell as? mxCell)?.value as? FlowNode ?: return result
        when (flow.currentMatchResult?.status) {
            FlowMatchStatus.IGNORED -> result += DiagramStyles.IGNORED
            FlowMatchStatus.FAIL -> result += DiagramStyles.FAILED
            FlowMatchStatus.PARTIAL -> result += DiagramStyles.PARTIAL_MATCH
            FlowMatchStatus.SUCCESS -> result += DiagramStyles.SUCCESS
            null -> {}
        }
        if (flow.searchHighlight) {
            result += DiagramStyles.SEARCH_HIGHLIGHT
        }
        return result
    }
}

private suspend fun addGraphContent(
    graph: mxGraph,
    flowGraph: FlowGraph
): SortedMap<FlowGroup, List<mxCell>> {
    val groupedCells = sortedMapOf<FlowGroup, List<mxCell>>()
    graph.update {
        fun addFlow(flow: FlowNode, parent: mxCell?, out: (mxCell) -> Unit) {
            val node = graph.insertVertex(null, null, flow, 0.0, 0.0, 0.0, 0.0) as mxCell
            graph.updateCellSize(node, true)
            if (parent != null) {
                out(graph.insertEdge(null, null, null, node, parent) as mxCell)
            }
            for (input in flow.inputs) {
                addFlow(input, node, out)
            }
            out(node)
        }

        for (group in flowGraph) {
            checkCanceled()
            val cells = mutableListOf<mxCell>()
            addFlow(group.root, null, cells::add)
            groupedCells[group] = cells
        }
    }
    return groupedCells
}

private suspend fun layOutGraph(
    graph: mxGraph,
    groupedCells: SortedMap<FlowGroup, List<mxCell>>,
    lineNumberNodes: SortedMap<Int, mxCell>
): () -> Dimension {
    val layout = mxHierarchicalLayout(graph)
    var lastBounds = mxRectangle(0.0, 0.0, 0.0, 0.0)
    var maxX = 0.0
    var maxY = 0.0
    var lastLine: Int? = null
    for ((group, list) in groupedCells) {
        checkCanceled()

        val (targetLeft, targetTop) = if (group.lineNumber == lastLine) {
            (lastBounds.x + lastBounds.width + INTRA_GROUP_SPACING) to (lastBounds.y)
        } else {
            val label = graph.insertVertex(
                null, null,
                "Line ${group.lineNumber}:",
                OUTER_PADDING / 2,
                maxY + INTER_GROUP_SPACING / 2,
                0.0, 0.0,
                LINE_NUMBER_STYLE
            ) as mxCell
            lineNumberNodes[group.lineNumber] = label
            graph.updateCellSize(label, true)
            graph.moveCells(arrayOf(label), 0.0, -graph.view.getState(label).height / 2)
            (OUTER_PADDING) to (maxY + INTER_GROUP_SPACING)
        }
        layout.execute(graph.getDefaultParent(), list)
        val cells = list.toTypedArray()
        val bounds = graph.view.getBounds(cells)
        graph.moveCells(cells, -bounds.x + targetLeft, -bounds.y + targetTop)
        lastBounds = mxRectangle(targetLeft, targetTop, bounds.width, bounds.height)
        maxX = maxOf(maxX, lastBounds.x + lastBounds.width)
        maxY = maxOf(maxY, lastBounds.y + lastBounds.height)
        lastLine = group.lineNumber
    }

    return {
        Dimension(
            ((maxX + OUTER_PADDING) * graph.view.scale).toInt(),
            ((maxY + OUTER_PADDING) * graph.view.scale).toInt()
        )
    }
}

private fun setupStyles(graph: mxGraph) {
    val stylesheet = graph.stylesheet
    stylesheet.defaultVertexStyle.putAll(DiagramStyles.DEFAULT_NODE)
    stylesheet.defaultEdgeStyle.putAll(DiagramStyles.DEFAULT_EDGE)
    stylesheet.putCellStyle(LINE_NUMBER_STYLE, DiagramStyles.LINE_NUMBER)
}
