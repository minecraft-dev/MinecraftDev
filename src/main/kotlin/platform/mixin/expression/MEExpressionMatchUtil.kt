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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MemberInfo
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.resolveType
import com.demonwav.mcdev.util.resolveTypeArray
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiModifierList
import com.llamalad7.mixinextras.expression.impl.ExpressionParserFacade
import com.llamalad7.mixinextras.expression.impl.ExpressionService
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression
import com.llamalad7.mixinextras.expression.impl.flow.ComplexDataException
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import com.llamalad7.mixinextras.expression.impl.pool.SimpleMemberDefinition
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer

typealias IdentifierPoolFactory = (MethodNode) -> IdentifierPool
typealias FlowMap = Map<VirtualInsn, FlowValue>

/**
 * An instruction that MixinExtras generates (via instruction expansion), as opposed to an instruction in the original
 * method. One type of instruction cannot be directly assigned to another, to avoid a method instruction being used when
 * a virtual instruction is expected and vice versa.
 */
@JvmInline
value class VirtualInsn(val insn: AbstractInsnNode)

object MEExpressionMatchUtil {
    private val LOGGER = logger<MEExpressionMatchUtil>()

    init {
        ExpressionService.offerInstance(MEExpressionService)
    }

    private val flowCache = Caffeine.newBuilder().weakKeys().build<MethodNode, FlowMap?>()

    fun getFlowMap(project: Project, classNode: ClassNode, methodIn: MethodNode): FlowMap? {
        if (methodIn.instructions == null) {
            return null
        }

        return flowCache.asMap().computeIfAbsent(methodIn) { methodNode ->
            val interpreter = object : FlowInterpreter(classNode, methodNode, MEFlowContext(project)) {
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
                return@computeIfAbsent null
            }

            interpreter.finish().asSequence().mapNotNull { flow -> flow.virtualInsnOrNull?.let { it to flow } }.toMap()
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

            val fields = annotation.findDeclaredAttributeValue("field")?.computeStringArray() ?: emptyList()
            for (field in fields) {
                val fieldRef = MemberInfo.parse(field) ?: continue
                pool.addMember(
                    definitionId,
                    SimpleMemberDefinition {
                        it is FieldInsnNode && fieldRef.matchField(it.owner, it.name, it.desc)
                    }
                )
            }

            val methods = annotation.findDeclaredAttributeValue("method")?.computeStringArray() ?: emptyList()
            for (method in methods) {
                val methodRef = MemberInfo.parse(method) ?: continue
                pool.addMember(
                    definitionId,
                    object : SimpleMemberDefinition {
                        override fun matches(insn: AbstractInsnNode) =
                            insn is MethodInsnNode && methodRef.matchMethod(insn.owner, insn.name, insn.desc)

                        override fun matches(handle: Handle) =
                            handle.tag in Opcodes.H_INVOKEVIRTUAL..Opcodes.H_INVOKEINTERFACE &&
                                methodRef.matchMethod(handle.owner, handle.name, handle.desc)
                    }
                )
            }

            val types = annotation.findDeclaredAttributeValue("type")?.resolveTypeArray() ?: emptyList()
            for (type in types) {
                val asmType = Type.getType(type.descriptor)
                pool.addType(definitionId) { it == asmType }
            }

            val locals = annotation.findDeclaredAttributeValue("local")?.findAnnotations() ?: emptyList()
            for (localAnnotation in locals) {
                val localType = localAnnotation.findDeclaredAttributeValue("type")?.resolveType()
                val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)
                pool.addMember(definitionId) { node ->
                    val virtualInsn = node.insn
                    if (virtualInsn !is VarInsnNode) {
                        return@addMember false
                    }
                    val physicalInsn = InsnExpander.getRepresentative(node)
                    val actualInsn = if (virtualInsn.opcode >= Opcodes.ISTORE && virtualInsn.opcode <= Opcodes.ASTORE) {
                        physicalInsn.next ?: return@addMember false
                    } else {
                        physicalInsn
                    }

                    val filteredLocals = localInfo.matchLocals(
                        module, targetClass, targetMethod, actualInsn,
                        CollectVisitor.Mode.RESOLUTION
                    ) ?: return@addMember false
                    filteredLocals.any { it.index == virtualInsn.`var` }
                }
            }
        }

        pool
    }

    fun createExpression(text: String): Expression? {
        return try {
            ExpressionParserFacade.parse(text)
        } catch (e: Exception) {
            null
        } catch (e: StackOverflowError) {
            null
        }
    }

    fun getContextType(project: Project, annotationName: String?): ExpressionContext.Type {
        if (annotationName == null) {
            return ExpressionContext.Type.CUSTOM
        }
        if (annotationName == MixinConstants.Annotations.SLICE) {
            return ExpressionContext.Type.SLICE
        }

        val handler = MixinAnnotationHandler.forMixinAnnotation(annotationName, project) as? InjectorAnnotationHandler
            ?: return ExpressionContext.Type.CUSTOM
        return handler.mixinExtrasExpressionContextType
    }

    inline fun findMatchingInstructions(
        targetClass: ClassNode,
        targetMethod: MethodNode,
        pool: IdentifierPool,
        flows: FlowMap,
        expr: Expression,
        insns: Iterable<VirtualInsn>,
        contextType: ExpressionContext.Type,
        forCompletion: Boolean,
        crossinline reportMatchStatus: (FlowValue, Expression, Boolean) -> Unit = { _, _, _ -> },
        crossinline reportPartialMatch: (FlowValue, Expression) -> Unit = { _, _ -> },
        callback: (ExpressionMatch) -> Unit
    ) {
        for (insn in insns) {
            val decorations = mutableMapOf<VirtualInsn, MutableMap<String, Any?>>()
            val captured = mutableListOf<Pair<FlowValue, Int>>()

            val sink = object : Expression.OutputSink {
                override fun capture(node: FlowValue, expr: Expression?, ctx: ExpressionContext?) {
                    captured += node to (expr?.src?.startIndex ?: 0)
                    decorations.getOrPut(insn, ::mutableMapOf).putAll(node.decorations)
                }

                override fun decorate(insn: AbstractInsnNode, key: String, value: Any?) {
                    decorations.getOrPut(VirtualInsn(insn), ::mutableMapOf)[key] = value
                }

                override fun decorateInjectorSpecific(insn: AbstractInsnNode, key: String, value: Any?) {
                    // Our maps are per-injector anyway, so this is just a normal decoration.
                    decorations.getOrPut(VirtualInsn(insn), ::mutableMapOf)[key] = value
                }

                override fun reportMatchStatus(node: FlowValue, expr: Expression, matched: Boolean) {
                    reportMatchStatus.invoke(node, expr, matched)
                }

                override fun reportPartialMatch(node: FlowValue, expr: Expression) {
                    reportPartialMatch.invoke(node, expr)
                }
            }

            val flow = flows[insn] ?: continue
            try {
                val context = ExpressionContext(pool, sink, targetClass, targetMethod, contextType, forCompletion)
                if (expr.matches(flow, context)) {
                    for ((capturedFlow, startOffset) in captured) {
                        val capturedInsn = capturedFlow.virtualInsnOrNull ?: continue
                        val originalInsn = InsnExpander.getRepresentative(capturedFlow) ?: capturedInsn.insn
                        callback(ExpressionMatch(flow, originalInsn, startOffset, decorations[capturedInsn].orEmpty()))
                    }
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (ignored: Exception) {
                // MixinExtras throws lots of different exceptions
            }
        }
    }

    val FlowValue.virtualInsn: VirtualInsn get() = VirtualInsn(insn)

    val FlowValue.virtualInsnOrNull: VirtualInsn? get() = try {
        VirtualInsn(insn)
    } catch (e: ComplexDataException) {
        null
    }

    class ExpressionMatch @PublishedApi internal constructor(
        val flow: FlowValue,
        val originalInsn: AbstractInsnNode,
        val startOffset: Int,
        val decorations: Map<String, Any?>,
    )
}
