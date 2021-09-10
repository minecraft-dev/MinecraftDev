/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtilRt

/**
 * The reference inside @At.target().
 *
 * See [AtResolver] for details as to how resolving @At references works.
 */
object TargetReference : PolyReferenceResolver(), MixinReference {

    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(AT, "target")

    override val description: String
        get() = "target reference '%s'"

    override fun isValidAnnotation(name: String) = name == AT

    fun resolveTarget(context: PsiElement): PsiMember? {
        val selector = parseMixinSelector(context) ?: return null
        return selector.resolveMember(context.project)
    }

    /**
     * Null is returned when no parent annotation handler could be found, in which case we shouldn't mark this
     * reference as unresolved.
     */
    private fun getTargets(at: PsiAnnotation): List<ClassAndMethodNode>? {
        val method = at.parentOfType<PsiMethod>() ?: return emptyList()
        val (handler, annotation) = method.annotations.mapFirstNotNull { annotation ->
            val qName = annotation.qualifiedName ?: return@mapFirstNotNull null
            MixinAnnotationHandler.forMixinAnnotation(qName)?.let { it to annotation }
        } ?: return null
        return handler.resolveTarget(annotation).mapNotNull { (it as? MethodTargetMember)?.classAndMethod }
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        val at = context.parentOfType<PsiAnnotation>() ?: return true
        val targets = getTargets(at)?.ifEmpty { return true } ?: return false
        return targets.all { AtResolver(at, it.clazz, it.method).isUnresolved() }
    }

    fun resolveNavigationTargets(context: PsiElement): Array<PsiElement>? {
        val at = context.parentOfType<PsiAnnotation>() ?: return null
        val targets = getTargets(at) ?: return null
        return targets.flatMap { AtResolver(at, it.clazz, it.method).resolveNavigationTargets() }.toTypedArray()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val result = resolveTarget(context) ?: return ResolveResult.EMPTY_ARRAY
        return arrayOf(PsiElementResolveResult(result))
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val at = context.parentOfType<PsiAnnotation>() ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        val targets = getTargets(at) ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        return targets.flatMap { target ->
            AtResolver(at, target.clazz, target.method).collectTargetVariants { builder ->
                builder.completeToLiteral(context)
            }
        }.toTypedArray()
    }
}
