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

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.reference.DescSelectorParser
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.getGenericParameterTypes
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.ifNullOrEmpty
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiModificationTracker
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import java.util.concurrent.ConcurrentHashMap
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class InjectorAnnotationHandler : MixinAnnotationHandler {
    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        val methodAttr = annotation.findAttributeValue("method")
        val method = methodAttr?.computeStringArray() ?: emptyList()
        val desc = annotation.findAttributeValue("desc")?.findAnnotations() ?: emptyList()
        val selectors = method.mapNotNull { parseMixinSelector(it, methodAttr!!) } +
            desc.mapNotNull { DescSelectorParser.descSelectorFromAnnotation(it) }

        val targetClassMethods = selectors.associateWith { selector ->
            val actualTarget = selector.getCustomOwner(targetClass)
            (actualTarget to actualTarget.methods)
        }

        return targetClassMethods.mapNotNull { (selector, pair) ->
            val (clazz, methods) = pair
            methods.firstNotNullOfOrNull { method ->
                if (selector.matchMethod(method, clazz)) {
                    MethodTargetMember(clazz, method)
                } else {
                    null
                }
            }
        }
    }

    override fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): InsnResolutionInfo.Failure? {
        // check for misc dynamic selectors in method
        val methodAttr = annotation.findAttributeValue("method")
        if (methodAttr?.computeStringArray()?.any { isMiscDynamicSelector(annotation.project, it) } == true) {
            return null
        }

        return resolveTarget(annotation, targetClass).map { targetMember ->
            val targetMethod = targetMember as? MethodTargetMember ?: return@map InsnResolutionInfo.Failure()
            isUnresolved(annotation, targetClass, targetMethod.classAndMethod.method) ?: return@isUnresolved null
        }.reduceOrNull(InsnResolutionInfo.Failure::combine) ?: InsnResolutionInfo.Failure()
    }

    open fun getAtKey(annotation: PsiAnnotation): String = "at"

    protected open fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): InsnResolutionInfo.Failure? {
        return annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
            .ifNullOrEmpty { return InsnResolutionInfo.Failure() }!!
            .firstNotNullOfOrNull { AtResolver(it, targetClass, targetMethod).isUnresolved() }
    }

    override fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement> {
        return resolveTarget(annotation, targetClass).flatMap { targetMember ->
            val targetMethod = targetMember as? MethodTargetMember ?: return@flatMap emptyList()
            resolveForNavigation(annotation, targetMethod.classAndMethod.clazz, targetMethod.classAndMethod.method)
        }
    }

    protected open fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): List<PsiElement> {
        return annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
            .ifNullOrEmpty { return emptyList() }!!
            .flatMap { AtResolver(it, targetClass, targetMethod).resolveNavigationTargets() }
    }

    fun resolveInstructions(annotation: PsiAnnotation) = annotation.cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val containingClass = annotation.findContainingClass() ?: return@cached emptyList()
        containingClass.mixinTargets.flatMap { resolveInstructions(annotation, it) }
    }

    fun resolveInstructions(annotation: PsiAnnotation, targetClass: ClassNode): List<InsnResult> {
        return resolveTarget(annotation, targetClass)
            .flatMap { targetMember ->
                val targetMethod = (targetMember as? MethodTargetMember)?.classAndMethod ?: return@flatMap emptyList()
                resolveInstructions(annotation, targetMethod.clazz, targetMethod.method).map { result ->
                    InsnResult(targetMethod, result)
                }
            }
    }

    open fun resolveInstructions(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        mode: CollectVisitor.Mode = CollectVisitor.Mode.MATCH_ALL,
    ): List<CollectVisitor.Result<*>> {
        val cache = annotation.cached(PsiModificationTracker.MODIFICATION_COUNT) {
            ConcurrentHashMap<Pair<ClassAndMethodNode, CollectVisitor.Mode>, List<CollectVisitor.Result<*>>>()
        }
        return cache.computeIfAbsent(ClassAndMethodNode(targetClass, targetMethod) to mode) {
            annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
                .ifNullOrEmpty { return@computeIfAbsent emptyList() }!!
                .flatMap { AtResolver(it, targetClass, targetMethod).resolveInstructions(mode) }
        }
    }

    /**
     * Returns a list of valid method signatures for the injector.
     * May return an empty list for no valid signatures, or null for all signatures being valid.
     * Null is usually returned when an error is detected, which is better handled by another inspection.
     */
    abstract fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ): List<MethodSignature>?

    open fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return true
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        return "Cannot resolve any target instructions in target class"
    }

    override fun createTargetInlay(
        context: MixinAnnotationHandler.TargetInlayContext
    ): MixinAnnotationHandler.TargetInlayProperties? {
        val at = context.annotation.findAttributeValue(getAtKey(context.annotation))
            ?.findAnnotations()
            ?.getOrNull(context.navigationIndex)
            ?: return null
        return AtResolver.getInjectionPoint(at)?.createTargetInlay(at, context)
    }

    protected open val defaultOrder = MixinConstants.InjectorOrder.DEFAULT

    fun getOrder(annotation: PsiAnnotation): Long {
        return (annotation.findAttributeValue("order")?.constantValue as? Int)?.toLong() ?: defaultOrder
    }

    open val allowCoerce = false

    override val isEntryPoint = true

    override val icon = MixinAssets.MIXIN_INJECTOR_ICON

    abstract val mixinExtrasExpressionContextType: ExpressionContext.Type

    data class InsnResult(val method: ClassAndMethodNode, val result: CollectVisitor.Result<*>)

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
                    val name = localVariables
                        ?.getOrNull(index + numLocalsToDrop)
                        ?.name
                        ?.toJavaIdentifier()
                        ?: "par${index + 1}"
                    type to name
                }
                .map { (type, name) -> sanitizedParameter(type, name) }
                .toList()
        }

        @JvmStatic
        protected fun sanitizedParameter(type: PsiType, name: String?): Parameter {
            // Parameters should not use ellipsis because others like CallbackInfo may follow
            return if (type is PsiEllipsisType) {
                Parameter(name?.toJavaIdentifier(), type.toArrayType())
            } else {
                Parameter(name?.toJavaIdentifier(), type)
            }
        }
    }
}

object DefaultInjectorAnnotationHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
    ) = null

    override val isSoft = true

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.CUSTOM
}
