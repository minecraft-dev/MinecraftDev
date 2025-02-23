/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.callbackInfoReturnableType
import com.demonwav.mcdev.platform.mixin.util.callbackInfoType
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.McdevDfaUtil
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.firstIndexOrNull
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiStatement
import com.intellij.psi.PsiTypes
import com.intellij.psi.controlFlow.ConditionalThrowToInstruction
import com.intellij.psi.controlFlow.ControlFlow
import com.intellij.psi.controlFlow.ControlFlowFactory
import com.intellij.psi.controlFlow.ControlFlowUtil
import com.intellij.psi.controlFlow.LocalsOrMyInstanceFieldsControlFlowPolicy
import com.intellij.psi.controlFlow.ReturnInstruction
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import com.intellij.util.takeWhileInclusive
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.siyeh.ig.psiutils.SideEffectChecker
import java.util.BitSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class InjectAnnotationHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): List<MethodSignature> {
        val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)

        val result = ArrayList<ParameterGroup>()

        // Parameters from injected method (optional)
        result.add(
            ParameterGroup(
                collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
                required = ParameterGroup.RequiredLevel.OPTIONAL,
                default = true,
            ),
        )

        // Callback info (required)
        result.add(
            ParameterGroup(
                listOf(
                    if (returnType == PsiTypes.voidType()) {
                        Parameter("ci", callbackInfoType(annotation.project))
                    } else {
                        Parameter(
                            "cir",
                            callbackInfoReturnableType(annotation.project, annotation, returnType)!!,
                        )
                    },
                ),
            ),
        )

        // Captured locals (only if local capture is enabled)
        val localCapture = (annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)
            ?.referenceName ?: "NO_CAPTURE"
        if (localCapture != "NO_CAPTURE") {
            annotation.findModule()?.let { module ->
                var commonLocalsPrefix: MutableList<LocalVariables.LocalVariable>? = null
                val resolvedInsns = resolveInstructions(annotation, targetClass, targetMethod).ifEmpty { return@let }
                for (insn in resolvedInsns) {
                    val locals = LocalVariables.getLocals(module, targetClass, targetMethod, insn.insn)
                        ?.filterNotNull()
                        ?.drop(
                            Type.getArgumentTypes(targetMethod.desc).size +
                                if (targetMethod.hasAccess(Opcodes.ACC_STATIC)) 0 else 1,
                        )
                        ?.filter { it.desc != null }
                        ?: continue
                    if (commonLocalsPrefix == null) {
                        commonLocalsPrefix = locals.toMutableList()
                    } else {
                        val mismatch = commonLocalsPrefix.zip(locals).firstIndexOrNull { (a, b) -> a.desc != b.desc }
                        if (mismatch != null) {
                            commonLocalsPrefix.subList(mismatch, commonLocalsPrefix.size).clear()
                        }
                    }
                }

                if (commonLocalsPrefix != null) {
                    val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
                    val localParams = commonLocalsPrefix.map { local ->
                        val type =
                            Type.getType(local.desc).toPsiType(elementFactory, annotation.parentOfType<PsiMethod>())
                        sanitizedParameter(type, local.name)
                    }
                    val requiredLevel = if (localCapture == "CAPTURE_FAILSOFT") {
                        ParameterGroup.RequiredLevel.WARN_IF_ABSENT
                    } else {
                        ParameterGroup.RequiredLevel.ERROR_IF_ABSENT
                    }
                    result.add(
                        ParameterGroup(
                            localParams,
                            default = true,
                            required = requiredLevel,
                            isVarargs = true,
                        ),
                    )
                }
            }
        }

        return listOf(MethodSignature(result, PsiTypes.voidType()))
    }

    override val allowCoerce = true

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.INJECT

    override fun createTargetInlay(
        context: MixinAnnotationHandler.TargetInlayContext
    ): MixinAnnotationHandler.TargetInlayProperties? {
        val inlayProps = super.createTargetInlay(context) ?: return null
        val at = context.annotation.findAttributeValue("at")?.findAnnotations()?.getOrNull(context.navigationIndex)
            ?: return null
        return moveInlayAcrossNonSideEffectCodeToPrettierSpot(context, AtResolver.getShift(at) > 0, inlayProps)
    }

    companion object {
        /**
         * Note: this doesn't just have to check for side effects, it also has to check for whether where we're shifting
         * to is reached iff the injection point is reached.
         */
        fun moveInlayAcrossNonSideEffectCodeToPrettierSpot(
            context: MixinAnnotationHandler.TargetInlayContext,
            isAfter: Boolean,
            inlayProps: MixinAnnotationHandler.TargetInlayProperties
        ): MixinAnnotationHandler.TargetInlayProperties {
            val targetElement = context.targetElement

            if (
                inlayProps.placement != MixinAnnotationHandler.TargetInlayPlacement.BEFORE &&
                inlayProps.placement != MixinAnnotationHandler.TargetInlayPlacement.AFTER
            ) {
                return inlayProps
            }

            val controlFlowBlock = McdevDfaUtil.getControlFlowContext(targetElement) ?: return inlayProps
            val project = controlFlowBlock.project
            val controlFlow = ControlFlowFactory.getInstance(project)
                .getControlFlow(controlFlowBlock, LocalsOrMyInstanceFieldsControlFlowPolicy.getInstance())

            val topmostParent = targetElement.parents(withSelf = true)
                .takeWhile { it !is PsiClass && (it !is PsiMember || it is PsiField) && it !is PsiLambdaExpression }
                .firstOrNull { it is PsiStatement || it is PsiField }
                ?: targetElement.parents(withSelf = true).takeWhile { it is PsiExpression }.lastOrNull()
                ?: targetElement
            val parents = targetElement.parents(withSelf = true).takeWhileInclusive { it != topmostParent }.toList()

            // workaround for SideEffectChecker requiring PsiExpression for all methods which take a list
            val sideEffects = mutableListOf<PsiElement>()
            topmostParent.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (SideEffectChecker.mayHaveSideEffects(element) { it != element }) {
                        sideEffects += element
                    }
                    super.visitElement(element)
                }

                override fun visitExpression(expression: PsiExpression) {
                    SideEffectChecker.checkSideEffects(expression, sideEffects)
                }
            })
            val sideEffectOffsets = BitSet()
            for (sideEffect in sideEffects) {
                val offset = controlFlow.getEndOffset(sideEffect)
                if (offset >= 0) {
                    sideEffectOffsets.set(offset)
                }
            }

            var anchor = inlayProps.anchor

            if (isAfter) {
                val targetOffset = controlFlow.getEndOffset(targetElement)
                if (targetOffset >= 0) {
                    val afterAnchor = parents.takeWhile { parent ->
                        if (!PsiTreeUtil.isAncestor(controlFlowBlock, parent, false)) {
                            return@takeWhile true
                        }
                        val endOffset = controlFlow.getEndOffset(parent)
                        if (endOffset < 0) {
                            return@takeWhile true
                        }
                        isAlwaysReachedAndNotViaSideEffects(
                            controlFlow,
                            targetOffset,
                            endOffset,
                            sideEffectOffsets,
                            skipFirst = true
                        )
                    }.lastOrNull()
                    if (afterAnchor != null) {
                        anchor = afterAnchor
                    }
                }
            } else {
                val targetOffset = controlFlow.getStartOffset(targetElement)
                if (targetOffset >= 0) {
                    val beforeAnchor = parents.takeWhile { parent ->
                        if (!PsiTreeUtil.isAncestor(controlFlowBlock, parent, false)) {
                            return@takeWhile true
                        }
                        val startOffset = controlFlow.getStartOffset(parent)
                        if (startOffset < 0) {
                            return@takeWhile true
                        }
                        isAlwaysReachedAndNotViaSideEffects(
                            controlFlow,
                            startOffset,
                            targetOffset,
                            sideEffectOffsets,
                            skipFirst = false
                        )
                    }.lastOrNull()
                    if (beforeAnchor != null) {
                        anchor = beforeAnchor
                    }
                }
            }

            val placement = if (anchor is PsiStatement || anchor is PsiField) {
                if (isAfter) {
                    MixinAnnotationHandler.TargetInlayPlacement.NEXT_LINE
                } else {
                    MixinAnnotationHandler.TargetInlayPlacement.PREVIOUS_LINE
                }
            } else {
                inlayProps.placement
            }

            return inlayProps.copy(anchor = anchor, placement = placement)
        }

        private fun isAlwaysReachedAndNotViaSideEffects(
            controlFlow: ControlFlow,
            from: Int,
            to: Int,
            sideEffects: BitSet,
            skipFirst: Boolean,
        ): Boolean {
            val graph = ControlFlowUtil.getEdges(controlFlow, 0).groupBy({ it.myFrom }) { it.myTo }

            val visited = BitSet()
            fun checkToUnreachableNotViaFrom(index: Int): Boolean {
                if (index == from) {
                    return true
                }
                if (index == to) {
                    return false
                }
                if (visited.get(index)) {
                    return true
                }
                visited.set(index)
                val successors = graph[index] ?: return true
                return successors.all { checkToUnreachableNotViaFrom(it) }
            }
            if (!checkToUnreachableNotViaFrom(0)) {
                return false
            }

            visited.clear()
            fun dfs(index: Int): Boolean {
                if (index == to || visited.get(index)) {
                    return true
                }
                if (index < 0 || index >= controlFlow.instructions.size) {
                    return false
                }

                visited.set(index)

                val insn = controlFlow.instructions[index]
                val successors = if (insn is ConditionalThrowToInstruction) {
                    listOf(index + 1)
                } else {
                    graph[index] ?: emptyList()
                }

                if (!skipFirst || index != from) {
                    if (sideEffects.get(index) || insn is ReturnInstruction) {
                        return false
                    }
                }

                return successors.all { dfs(it) }
            }

            return dfs(from)
        }
    }
}
