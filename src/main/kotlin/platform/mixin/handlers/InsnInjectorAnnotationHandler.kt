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

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.TargetInsn
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.ifNullOrEmpty
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiModificationTracker
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import java.util.concurrent.ConcurrentHashMap
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class InsnInjectorAnnotationHandler : InjectorAnnotationHandler() {
    open fun getAtKey(annotation: PsiAnnotation): String = "at"

    final override fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): InsnResolutionInfo.Failure? {
        val results = annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
            .ifNullOrEmpty { return InsnResolutionInfo.Failure(AtResolver.DEFAULT_UNRESOLVED_MESSAGE) }
            .map { AtResolver(it, targetClass, targetMethod).isUnresolved() }
        return if (null in results) null else results.firstNotNullOf { it }
    }

    final override fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<PsiElement> {
        return annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
            .ifNullOrEmpty { return emptyList() }
            .flatMap { AtResolver(it, targetClass, targetMethod).resolveNavigationTargets() }
    }

    fun resolveInstructions(annotation: PsiAnnotation) = annotation.cached(PsiModificationTracker.MODIFICATION_COUNT) {
        val containingClass = annotation.findContainingClass() ?: return@cached emptyList()
        containingClass.mixinTargets.flatMap { resolveInstructions(annotation, it) }
    }

    fun resolveInstructions(annotation: PsiAnnotation, targetClass: ClassNode): List<InsnResult> {
        return resolveInstructions(annotation, resolveTarget(annotation, targetClass).map { it.classAndMethod })
    }

    fun resolveInstructions(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>,
        mode: CollectVisitor.Mode = CollectVisitor.Mode.RESOLUTION,
    ): List<InsnResult> {
        return targets.flatMap { (targetClass, targetMethod) ->
            resolveInstructions(
                annotation,
                targetClass,
                targetMethod,
                mode,
            ).map { InsnResult(ClassAndMethodNode(targetClass, targetMethod), it) }
        }
    }

    open fun resolveInstructions(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        mode: CollectVisitor.Mode = CollectVisitor.Mode.RESOLUTION,
    ): List<CollectVisitor.Result<*>> {
        val cache = annotation.cached(PsiModificationTracker.MODIFICATION_COUNT) {
            ConcurrentHashMap<Pair<ClassAndMethodNode, CollectVisitor.Mode>, List<CollectVisitor.Result<*>>>()
        }
        return cache.computeIfAbsent(ClassAndMethodNode(targetClass, targetMethod) to mode) {
            annotation.findAttributeValue(getAtKey(annotation))?.findAnnotations()
                .ifNullOrEmpty { return@computeIfAbsent emptyList() }
                .flatMap { AtResolver(it, targetClass, targetMethod).resolveInstructions(mode) }
        }
    }

    final override fun expectedMethodSignatures(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>,
        mode: CollectVisitor.Mode,
    ): List<ExpectedSignatures<*>> {
        return resolveInstructions(annotation, targets, mode).map { (target, result) ->
            val (targetClass, targetMethod) = target
            val targetInsn = TargetInsn(result.insn, result.decorations)
            expectedMethodSignature(annotation, targetClass, targetMethod, targetInsn)
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
        targetInsn: TargetInsn,
    ): ExpectedSignatures<*>

    open fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        return true
    }

    open val allowedInsnDescription = "all instructions"

    open val isShiftAlwaysDiscouraged = true

    abstract val mixinExtrasExpressionContextType: ExpressionContext.Type

    data class InsnResult(val method: ClassAndMethodNode, val result: CollectVisitor.Result<*>)
}
