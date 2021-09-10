/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.reference.DescSelectorParser
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

open class InjectorAnnotationHandler : MixinAnnotationHandler {
    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        val targetClassMethods = targetClass.methods ?: return emptyList()

        val methodAttr = annotation.findAttributeValue("method")
        val method = methodAttr?.computeStringArray() ?: emptyList()
        val desc = annotation.findAttributeValue("desc")?.findAnnotations() ?: emptyList()
        val selectors = method.mapNotNull { parseMixinSelector(it, methodAttr!!) } +
            desc.mapNotNull { DescSelectorParser.descSelectorFromAnnotation(it) }

        return targetClassMethods.mapNotNull { targetMethod ->
            if (selectors.any { it.matchMethod(targetMethod, targetClass) }) {
                MethodTargetMember(targetClass, targetMethod)
            } else {
                null
            }
        }
    }

    override fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): Boolean {
        if (resolveTarget(annotation, targetClass).any { targetMember ->
            val targetMethod = targetMember as? MethodTargetMember ?: return@any false
            !isUnresolved(annotation, targetClass, targetMethod.classAndMethod.method)
        }
        ) {
            return false
        }

        // check for misc dynamic selectors
        val methodAttr = annotation.findAttributeValue("method") ?: return true
        return !methodAttr.computeStringArray().any { isMiscDynamicSelector(annotation.project, it) }
    }

    protected open fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): Boolean {
        val at = annotation.findAttributeValue("at") as? PsiAnnotation ?: return true
        return AtResolver(at, targetClass, targetMethod,).isUnresolved()
    }

    override fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement> {
        return resolveTarget(annotation, targetClass).flatMap { targetMember ->
            val targetMethod = targetMember as? MethodTargetMember ?: return@flatMap emptyList()
            resolveForNavigation(annotation, targetClass, targetMethod.classAndMethod.method)
        }
    }

    protected open fun resolveForNavigation(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<PsiElement> {
        val at = annotation.findAttributeValue("at") as? PsiAnnotation ?: return emptyList()
        return AtResolver(at, targetClass, targetMethod).resolveNavigationTargets()
    }

    fun resolveInstructions(annotation: PsiAnnotation): List<InsnResult> {
        val containingClass = annotation.findContainingClass() ?: return emptyList()
        return containingClass.mixinTargets.flatMap { resolveInstructions(annotation, it) }
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
        targetMethod: MethodNode
    ): List<CollectVisitor.Result<*>> {
        val at = annotation.findAttributeValue("at") as? PsiAnnotation ?: return emptyList()
        return AtResolver(at, targetClass, targetMethod).resolveInstructions()
    }

    open fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return true
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation, unresolvedTargetClasses: String): String? {
        return "Cannot resolve any target instructions in target class $unresolvedTargetClasses"
    }

    data class InsnResult(val method: ClassAndMethodNode, val result: CollectVisitor.Result<*>)
}
