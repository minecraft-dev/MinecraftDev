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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.TargetInsn
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.reference.DescSelectorParser
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.platform.mixin.util.getGenericParameterTypes
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.util.findParentOfType
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class InjectorAnnotationHandler : MixinAnnotationHandler {
    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MethodTargetMember> {
        val methodAttr = annotation.findAttributeValue("method")
        val method = methodAttr?.computeStringArray() ?: emptyList()
        val desc = annotation.findAttributeValue("desc")?.findAnnotations() ?: emptyList()
        val selectors = method.mapNotNull { parseMixinSelector(it, methodAttr!!) } +
            desc.mapNotNull { DescSelectorParser.Util.descSelectorFromAnnotation(it) }

        val targetsBySelector = selectors.associateWith { selector ->
            selector.getCustomOwner(targetClass)
        }
        val allowStatic = annotation.findParentOfType<PsiMethod>()?.hasModifierProperty(PsiModifier.STATIC) ?: true

        return targetsBySelector.asSequence()
            .flatMap { (selector, targetClass) ->
                targetClass.findMethods(selector, allowStatic)
                    .map { ClassAndMethodNode(targetClass, it) }
            }
            .distinct()
            .map { MethodTargetMember(it) }
            .toList()
    }

    override fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): InsnResolutionInfo.Failure? {
        // check for misc dynamic selectors in method
        val methodAttr = annotation.findAttributeValue("method")
        if (methodAttr?.computeStringArray()?.any { isMiscDynamicSelector(annotation.project, it) } == true) {
            return null
        }

        return resolveTarget(annotation, targetClass).map { targetMethod ->
            isUnresolved(annotation, targetClass, targetMethod.classAndMethod.method) ?: return@isUnresolved null
        }.reduceOrNull(InsnResolutionInfo.Failure::combine) ?: InsnResolutionInfo.Failure(AtResolver.DEFAULT_UNRESOLVED_MESSAGE)
    }

    protected abstract fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): InsnResolutionInfo.Failure?

    override fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement> {
        return resolveTarget(annotation, targetClass).flatMap { targetMethod ->
            resolveForNavigation(annotation, targetMethod.classAndMethod.clazz, targetMethod.classAndMethod.method)
        }
    }

    protected abstract fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): List<PsiElement>

    abstract fun expectedMethodSignatures(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>,
        mode: CollectVisitor.Mode = CollectVisitor.Mode.SUGGESTION,
    ): List<ExpectedSignatures<*>>

    abstract fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>,
    ): SuggestedSignature?

    open fun canAlwaysBeStatic(method: PsiMethod): Boolean {
        return true
    }

    override val isImplicitlyUsed = true

    override val icon = MixinAssets.MIXIN_INJECTOR_ICON

    companion object {
        @JvmStatic
        protected fun collectTargetMethodParameters(
            project: Project,
            clazz: ClassNode,
            targetMethod: MethodNode,
        ): List<Parameter> {
            val numLocalsToDrop = if (targetMethod.hasAccess(Opcodes.ACC_STATIC)) 0 else 1
            val localVariables = targetMethod.localVariables?.sortedBy { it.index }
            return targetMethod.getGenericParameterTypes(clazz, project).asSequence().withIndex()
                .map { (index, type) ->
                    val knownName = localVariables
                        ?.getOrNull(index + numLocalsToDrop)
                        ?.name
                        ?.toJavaIdentifier()
                    val name = knownName ?: "par${index + 1}"
                    sanitizedParameter(type, name, knownName != null)
                }
                .toList()
        }

        @JvmStatic
        @JvmOverloads
        protected fun sanitizedParameter(type: PsiType, name: String?, knownName: Boolean = false): Parameter {
            // Parameters should not use ellipsis because others like CallbackInfo may follow
            return if (type is PsiEllipsisType) {
                Parameter(name?.toJavaIdentifier(), type.toArrayType(), knownName)
            } else {
                Parameter(name?.toJavaIdentifier(), type, knownName)
            }
        }
    }
}

object DefaultInjectorAnnotationHandler : InsnInjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        targetInsn: TargetInsn,
    ): ExpectedSignatures<Nothing> = ExpectedSignatures.Unknown

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? = null

    override val isSoft = true

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.CUSTOM
}
