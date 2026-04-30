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
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.reference.DescSelectorParser
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.ContextAwareMethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.getGenericParameterTypes
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.ifNullOrEmpty
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiMethod
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
            desc.mapNotNull { DescSelectorParser.Util.descSelectorFromAnnotation(it) }

        val targetClassMethods = selectors.associateWith { selector ->
            val actualTarget = selector.getCustomOwner(targetClass)
            (actualTarget to actualTarget.methods)
        }

        return targetClassMethods.mapNotNull { (selector, pair) ->
            val (clazz, methods) = pair
            methods.firstNotNullOfOrNull { method ->
                if (selector.matchMethod(method, clazz)) {
                    ContextAwareMethodTargetMember(
                        clazz,
                        method,
                        selector,
                    )
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
            val targetMethod = (targetMember as? MethodTargetMember)?.classAndMethod ?: return@map InsnResolutionInfo.Failure()
            isUnresolved(
                annotation,
                targetClass,
                targetMethod.method,
                (targetMember as? ContextAwareMethodTargetMember)?.selector
            ) ?: return@isUnresolved null
        }.reduceOrNull(InsnResolutionInfo.Failure::combine) ?: InsnResolutionInfo.Failure()
    }

    open fun getAtKey(annotation: PsiAnnotation): String = "at"

    protected open fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        enclosingSelector: MixinSelector?
    ): InsnResolutionInfo.Failure? {
        return annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
            .ifNullOrEmpty { return InsnResolutionInfo.Failure() }!!
            .firstNotNullOfOrNull { AtResolver(it, targetClass, targetMethod, enclosingSelector).isUnresolved() }
    }

    override fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement> {
        return resolveTarget(annotation, targetClass).flatMap { targetMember ->
            val targetMethod = (targetMember as? MethodTargetMember)?.classAndMethod ?: return@flatMap emptyList()
            resolveForNavigation(
                annotation,
                targetMethod.clazz,
                targetMethod.method,
                (targetMember as? ContextAwareMethodTargetMember)?.selector
            )
        }
    }

    protected open fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        enclosingSelector: MixinSelector?
    ): List<PsiElement> {
        return annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
            .ifNullOrEmpty { return emptyList() }!!
            .flatMap { AtResolver(it, targetClass, targetMethod, enclosingSelector).resolveNavigationTargets() }
    }

    fun resolveInstructions(annotation: PsiAnnotation) = annotation.cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val containingClass = annotation.findContainingClass() ?: return@cached emptyList()
        containingClass.mixinTargets.flatMap { resolveInstructions(annotation, it) }
    }

    fun resolveInstructions(annotation: PsiAnnotation, targetClass: ClassNode): List<InsnResult> {
        return resolveTarget(annotation, targetClass)
            .flatMap { targetMember ->
                val targetMethod = (targetMember as? MethodTargetMember)?.classAndMethod ?: return@flatMap emptyList()
                resolveInstructions(
                    annotation,
                    targetMethod.clazz,
                    targetMethod.method,
                    (targetMember as? ContextAwareMethodTargetMember)?.selector
                ).map { result ->
                    InsnResult(targetMethod, result)
                }
            }
    }

    open fun resolveInstructions(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        enclosingSelector: MixinSelector?,
        mode: CollectVisitor.Mode = CollectVisitor.Mode.RESOLUTION,
    ): List<CollectVisitor.Result<*>> {
        val cache = annotation.cached(PsiModificationTracker.MODIFICATION_COUNT) {
            ConcurrentHashMap<Pair<ClassAndMethodNode, CollectVisitor.Mode>, List<CollectVisitor.Result<*>>>()
        }
        return cache.computeIfAbsent(ClassAndMethodNode(targetClass, targetMethod) to mode) {
            annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
                .ifNullOrEmpty { return@computeIfAbsent emptyList() }!!
                .flatMap { AtResolver(it, targetClass, targetMethod, enclosingSelector).resolveInstructions(mode) }
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
        enclosingSelector: MixinSelector?
    ): List<MethodSignature>?

    open fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        return true
    }

    open val allowedInsnDescription = "all instructions"

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        return "Cannot resolve any target instructions in target class"
    }

    open fun canAlwaysBeStatic(method: PsiMethod): Boolean {
        return true
    }

    open val allowCoerce = false

    open val isShiftAlwaysDiscouraged = true

    override val isImplicitlyUsed = true

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

object DefaultInjectorAnnotationHandler : InjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        enclosingSelector: MixinSelector?,
    ) = null

    override val isSoft = true

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.CUSTOM
}
