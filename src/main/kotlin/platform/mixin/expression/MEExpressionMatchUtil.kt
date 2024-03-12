/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.cached
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.resolveType
import com.demonwav.mcdev.util.resolveTypeArray
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiModifierList
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import java.util.Collections
import java.util.IdentityHashMap
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer

typealias IdentifierPoolFactory = (MethodNode) -> IdentifierPool
typealias FlowMap = Map<AbstractInsnNode, FlowValue>

object MEExpressionMatchUtil {
    private val LOGGER = logger<MEExpressionMatchUtil>()

    fun getFlowMap(project: Project, classIn: ClassNode, methodIn: MethodNode): FlowMap? {
        if (methodIn.instructions == null) {
            return null
        }

        return methodIn.cached(classIn, project) { classNode, methodNode ->
            val interpreter = object : FlowInterpreter(classNode, methodNode) {
                override fun newValue(type: Type?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.newValue(type)
                }

                override fun newOperation(insn: AbstractInsnNode?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.newOperation(insn)
                }

                override fun copyOperation(insn: AbstractInsnNode?, value: FlowValue?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.copyOperation(insn, value)
                }

                override fun unaryOperation(insn: AbstractInsnNode?, value: FlowValue?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.unaryOperation(insn, value)
                }

                override fun binaryOperation(
                    insn: AbstractInsnNode?,
                    value1: FlowValue?,
                    value2: FlowValue?
                ): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.binaryOperation(insn, value1, value2)
                }

                override fun ternaryOperation(
                    insn: AbstractInsnNode?,
                    value1: FlowValue?,
                    value2: FlowValue?,
                    value3: FlowValue?
                ): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.ternaryOperation(insn, value1, value2, value3)
                }

                override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out FlowValue>?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.naryOperation(insn, values)
                }

                override fun returnOperation(insn: AbstractInsnNode?, value: FlowValue?, expected: FlowValue?) {
                    ProgressManager.checkCanceled()
                    super.returnOperation(insn, value, expected)
                }

                override fun merge(value1: FlowValue?, value2: FlowValue?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.merge(value1, value2)
                }
            }

            try {
                Analyzer(interpreter).analyze(classNode.name, methodNode)
            } catch (e: RuntimeException) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                LOGGER.warn("MEExpressionMatchUtil.getFlowMap failed", e)
                return@cached null
            }

            interpreter.finish()
        }
    }

    fun createIdentifierPoolFactory(
        module: Module,
        targetClass: ClassNode,
        modifierList: PsiModifierList,
    ): IdentifierPoolFactory = { targetMethod ->
        val pool = IdentifierPool()

        for (annotation in modifierList.annotations) {
            if (!annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)) {
                continue
            }

            val definitionId = annotation.findDeclaredAttributeValue("id")?.constantStringValue ?: ""

            val ats = annotation.findDeclaredAttributeValue("at")?.findAnnotations() ?: emptyList()
            for (at in ats) {
                val matchingInsns = RecursionManager.doPreventingRecursion(at, true) {
                    AtResolver(at, targetClass, targetMethod)
                        .resolveInstructions()
                        .mapTo(Collections.newSetFromMap(IdentityHashMap())) { it.insn }
                } ?: emptySet()
                pool.addMember(definitionId) { it in matchingInsns }
            }

            val types = annotation.findDeclaredAttributeValue("type")?.resolveTypeArray() ?: emptyList()
            for (type in types) {
                val asmType = Type.getType(type.descriptor)
                pool.addType(definitionId) { it == asmType }
            }

            val locals = annotation.findDeclaredAttributeValue("local")?.findAnnotations() ?: emptyList()
            for (localAnnotation in locals) {
                val localType = annotation.findDeclaredAttributeValue("type")?.resolveType()
                val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)
                pool.addMember(definitionId) { insn ->
                    if (insn !is VarInsnNode) {
                        return@addMember false
                    }
                    val actualInsn = if (insn.opcode >= Opcodes.ISTORE && insn.opcode <= Opcodes.ASTORE) {
                        insn.next ?: return@addMember false
                    } else {
                        insn
                    }

                    val unfilteredLocals = localInfo.getLocals(module, targetClass, targetMethod, actualInsn)
                        ?: return@addMember false
                    val filteredLocals = localInfo.matchLocals(unfilteredLocals, CollectVisitor.Mode.MATCH_ALL)
                    filteredLocals.any { it.index == insn.`var` }
                }
            }
        }

        pool
    }

    inline fun findMatchingInstructions(
        targetClass: ClassNode,
        targetMethod: MethodNode,
        pool: IdentifierPool,
        flows: FlowMap,
        expr: Expression,
        insns: Iterable<AbstractInsnNode>,
        forCompletion: Boolean,
        callback: (ExpressionMatch) -> Unit
    ) {
        for (insn in insns) {
            val decorations = IdentityHashMap<AbstractInsnNode, MutableMap<String, Any?>>()
            val captured = mutableListOf<Pair<FlowValue, Int>>()

            val sink = object : Expression.OutputSink {
                override fun capture(node: FlowValue, expr: Expression?) {
                    captured += node to (expr?.src?.startIndex ?: 0)
                    decorations.getOrPut(insn, ::mutableMapOf).putAll(node.decorations)
                }

                override fun decorate(insn: AbstractInsnNode, key: String, value: Any?) {
                    decorations.getOrPut(insn, ::mutableMapOf)[key] = value
                }

                override fun decorateInjectorSpecific(insn: AbstractInsnNode, key: String, value: Any?) {
                    // Our maps are per-injector anyway, so this is just a normal decoration.
                    decorations.getOrPut(insn, ::mutableMapOf)[key] = value
                }
            }

            val flow = flows[insn] ?: continue
            try {
                if (expr.matches(flow, ExpressionContext(pool, sink, targetClass, targetMethod, forCompletion))) {
                    for ((capturedFlow, startOffset) in captured) {
                        callback(ExpressionMatch(flow, startOffset, decorations[capturedFlow.insn].orEmpty()))
                    }
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (ignored: Exception) {
                // MixinExtras throws lots of different exceptions
            }
        }
    }

    class ExpressionMatch @PublishedApi internal constructor(
        val flow: FlowValue,
        val startOffset: Int,
        val decorations: Map<String, Any?>,
    )
}
