/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.inspection.injector.MethodSignature
import com.demonwav.mcdev.platform.mixin.reference.DescSelectorParser
import com.demonwav.mcdev.platform.mixin.reference.isMiscDynamicSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.getGenericParameterTypes
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.ifNullOrEmpty
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

abstract class InjectorAnnotationHandler : MixinAnnotationHandler {
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

    protected open fun isUnresolved(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): InsnResolutionInfo.Failure? {
        return annotation.findAttributeValue("at")?.findAnnotations()
            .ifNullOrEmpty { return InsnResolutionInfo.Failure() }!!
            .mapNotNull { AtResolver(it, targetClass, targetMethod).isUnresolved() }
            .firstOrNull()
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
        return annotation.findAttributeValue("at")?.findAnnotations()
            .ifNullOrEmpty { return emptyList() }!!
            .flatMap { AtResolver(it, targetClass, targetMethod).resolveNavigationTargets() }
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
        targetMethod: MethodNode,
        mode: CollectVisitor.Mode = CollectVisitor.Mode.MATCH_ALL
    ): List<CollectVisitor.Result<*>> {
        return annotation.findAttributeValue("at")?.findAnnotations()
            .ifNullOrEmpty { return emptyList() }!!
            .flatMap { AtResolver(it, targetClass, targetMethod).resolveInstructions(mode) }
    }

    /**
     * Returns a list of valid method signatures for the injector.
     * May return an empty list for no valid signatures, or null for all signatures being valid.
     * Null is usually returned when an error is detected, which is better handled by another inspection.
     */
    abstract fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode
    ): List<MethodSignature>?

    open fun isInsnAllowed(insn: AbstractInsnNode): Boolean {
        return true
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        return "Cannot resolve any target instructions in target class"
    }

    open val allowCoerce = false

    override val isEntryPoint = true

    data class InsnResult(val method: ClassAndMethodNode, val result: CollectVisitor.Result<*>)

    companion object {
        @JvmStatic
        protected fun collectTargetMethodParameters(
            project: Project,
            clazz: ClassNode,
            targetMethod: MethodNode
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
        targetMethod: MethodNode
    ) = null

    override val isSoft = true
}
