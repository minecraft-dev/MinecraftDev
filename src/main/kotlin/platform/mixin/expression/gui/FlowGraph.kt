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
import com.demonwav.mcdev.platform.mixin.expression.FlowMap
import com.demonwav.mcdev.platform.mixin.expression.MEExpressionMatchUtil
import com.intellij.openapi.application.readAction
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.llamalad7.mixinextras.expression.impl.ExpressionSource
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander
import java.util.Collections
import java.util.SortedSet
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode

enum class FlowMatchStatus {
    IGNORED, FAIL, PARTIAL, SUCCESS
}

data class FlowMatchResult(val status: FlowMatchStatus, val attempted: ExpressionSource?) : Comparable<FlowMatchResult> {
    override fun compareTo(other: FlowMatchResult) = status.compareTo(other.status)

    fun toString(prefix: String, suffix: String, transform: (String) -> String): String {
        val attempted = prefix + transform(StringUtil.escapeStringCharacters(attempted.toString())) + suffix
        return when (status) {
            FlowMatchStatus.IGNORED -> MCDevBundle("mixin.flow_diagram.status.ignored")
            FlowMatchStatus.FAIL -> MCDevBundle("mixin.flow_diagram.status.failed", attempted)
            FlowMatchStatus.PARTIAL -> MCDevBundle("mixin.flow_diagram.status.partial", attempted)
            FlowMatchStatus.SUCCESS -> MCDevBundle("mixin.flow_diagram.status.success", attempted)
        }
    }
}

class FlowNode(
    val flow: FlowValue,
    project: Project,
    clazz: ClassNode,
    method: MethodNode,
    map: MutableMap<FlowValue, FlowNode>
) {
    private val _matches =
        mutableMapOf<FlowValue, FlowMatchResult>().withDefault { FlowMatchResult(FlowMatchStatus.IGNORED, null) }
    var currentMatchResult: FlowMatchResult? = null
        private set
    val inputs = (0..<flow.inputCount()).map { FlowNode(flow.getInput(it), project, clazz, method, map) }
    val shortText = flow.shortString(project, clazz, method)
    val longText = flow.longString()
    var searchHighlight = false
    val matchScore get() = _matches.values.count { it.status >= FlowMatchStatus.PARTIAL }
    val matches: Collection<FlowMatchResult> get() = Collections.unmodifiableCollection(_matches.values)

    init {
        map[flow] = this
    }

    fun dfs(): Sequence<FlowNode> =
        sequenceOf(this) + inputs.asSequence().flatMap { it.dfs() }

    fun resetMatches() {
        _matches.clear()
        clearMatchHighlight()
    }

    fun clearMatchHighlight() {
        currentMatchResult = null
    }

    fun reportMatchStatus(childFlow: FlowValue, expr: Expression, matched: Boolean) {
        updateMatchStatus(
            childFlow,
            FlowMatchResult(
                if (matched) FlowMatchStatus.SUCCESS else FlowMatchStatus.FAIL,
                expr.src
            )
        )
    }

    fun reportPartialMatch(childFlow: FlowValue, expr: Expression) {
        updateMatchStatus(childFlow, FlowMatchResult(FlowMatchStatus.PARTIAL, expr.src))
    }

    private fun updateMatchStatus(childFlow: FlowValue, status: FlowMatchResult) {
        _matches.compute(childFlow) { _, oldStatus ->
            if (oldStatus == null) {
                status
            } else {
                maxOf(oldStatus, status)
            }
        }
    }

    fun highlightMatches(allNodes: Iterable<FlowNode>) {
        for (node in allNodes) {
            node.currentMatchResult = _matches.getValue(node.flow)
        }
    }
}

class FlowGroup(val root: FlowNode, method: MethodNode) : Comparable<FlowGroup> {
    private val startIndex = root.dfs()
        .map { it.flow }
        .filterNot { it.isComplex }
        .minOf {
            method.instructions.indexOf(InsnExpander.getRepresentative(it))
        }

    val lineNumber =
        generateSequence(method.instructions.get(startIndex)) { it.previous }
            .filterIsInstance<LineNumberNode>()
            .firstOrNull()?.line ?: -1

    override fun compareTo(other: FlowGroup) = compareValuesBy(this, other, { it.lineNumber }, { it.startIndex })
}

class FlowGraph(val groups: SortedSet<FlowGroup>, val flowMap: FlowMap, val allNodes: Map<FlowValue, FlowNode>) {
    var highlightRoot: FlowNode? = null
        private set
    private var hardHighlight = false
    private val highlightListeners = mutableListOf<(String, FlowNode?) -> Unit>()
    private var exprText: String? = null
    private val hasMatchData get() = exprText != null

    val orderedNodes get() = groups.asSequence().flatMap { it.root.dfs() }

    operator fun iterator() = groups.iterator()

    companion object {
        suspend fun parse(project: Project, clazz: ClassNode, method: MethodNode): FlowGraph? {
            val flows = readAction { MEExpressionMatchUtil.getFlowMap(project, clazz, method) } ?: return null
            val groups = sortedSetOf<FlowGroup>()
            val allNodes = mutableMapOf<FlowValue, FlowNode>()
            for (flow in flows.values) {
                if (!flow.isRoot) {
                    continue
                }
                checkCanceled()

                val node = FlowNode(flow, project, clazz, method, allNodes)
                groups.add(FlowGroup(node, method))
            }
            return FlowGraph(groups, flows, allNodes)
        }
    }

    fun resetMatches() {
        exprText = null
        highlightRoot = null
        hardHighlight = false
        for (node in allNodes.values) {
            node.resetMatches()
        }
    }

    fun setExprText(exprText: String) {
        this.exprText = exprText
    }

    fun highlightMatches(root: FlowNode?, soft: Boolean) {
        if (!hasMatchData) {
            return
        }
        if (hardHighlight && soft) {
            return
        }
        hardHighlight = root != null && !soft
        if (root == highlightRoot) {
            return
        }
        highlightRoot = root
        clearMatchHighlights()
        root?.highlightMatches(allNodes.values)

        // Fire listeners
        for (listener in highlightListeners) {
            listener(exprText!!, root)
        }
    }

    private fun clearMatchHighlights() {
        for (node in allNodes.values) {
            node.clearMatchHighlight()
        }
    }

    fun onHighlightChanged(listener: (String, FlowNode?) -> Unit) {
        highlightListeners += listener
    }

    fun shouldShowTooltips() = !hasMatchData || hardHighlight
}

private val FlowValue.isRoot get() = next.isEmpty()
