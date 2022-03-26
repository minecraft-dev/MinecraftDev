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

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import org.objectweb.asm.tree.ClassNode

interface MixinAnnotationHandler {
    fun resolveTarget(annotation: PsiAnnotation): List<MixinTargetMember> {
        val containingClass = annotation.findContainingClass() ?: return emptyList()
        return containingClass.mixinTargets.flatMap { resolveTarget(annotation, it) }
    }

    fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember>

    fun isUnresolved(annotation: PsiAnnotation): InsnResolutionInfo.Failure? {
        val containingClass = annotation.findContainingClass() ?: return InsnResolutionInfo.Failure()
        return containingClass.mixinTargets
            .mapNotNull { isUnresolved(annotation, it) }
            .reduceOrNull(InsnResolutionInfo.Failure::combine)
    }

    fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): InsnResolutionInfo.Failure?

    fun resolveForNavigation(annotation: PsiAnnotation): List<PsiElement> {
        val containingClass = annotation.findContainingClass() ?: return emptyList()
        return containingClass.mixinTargets.flatMap { resolveForNavigation(annotation, it) }
    }

    fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement>

    fun createUnresolvedMessage(annotation: PsiAnnotation): String?

    companion object {
        private val COLLECTOR =
            KeyedExtensionCollector<MixinAnnotationHandler, String>("com.demonwav.minecraft-dev.mixinAnnotationHandler")

        fun forMixinAnnotation(qualifiedName: String): MixinAnnotationHandler? {
            return COLLECTOR.findSingle(qualifiedName)
        }
    }
}

class MixinAnnotationHandlerInfo :
    BaseKeyedLazyInstance<MixinAnnotationHandler>(), KeyedLazyInstance<MixinAnnotationHandler> {

    @Attribute("annotation")
    @RequiredElement
    lateinit var annotation: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getKey(): String {
        return annotation
    }

    override fun getImplementationClassName(): String {
        return implementation
    }
}
