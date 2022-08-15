/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
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
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
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

    override fun isValidAnnotation(name: String, project: Project) = name == AT

    fun resolveTarget(context: PsiElement): PsiMember? {
        val selector = parseMixinSelector(context) ?: return null
        return selector.resolveMember(context.project)
    }

    /**
     * Null is returned when no parent annotation handler could be found, in which case we shouldn't mark this
     * reference as unresolved.
     */
    private fun getTargets(at: PsiAnnotation, forUnresolved: Boolean): List<ClassAndMethodNode>? {
        val (handler, annotation) = generateSequence(at.parent) { it.parent }
            .filterIsInstance<PsiAnnotation>()
            .mapNotNull { annotation ->
                val qName = annotation.qualifiedName ?: return@mapNotNull null
                MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project)?.let { it to annotation }
            }.firstOrNull() ?: return null
        if (forUnresolved && handler.isSoft) {
            return null
        }
        return handler.resolveTarget(annotation).mapNotNull { (it as? MethodTargetMember)?.classAndMethod }
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        val at = context.parentOfType<PsiAnnotation>() ?: return true
        val targets = getTargets(at, true)?.ifEmpty { return true } ?: return false
        return targets.all {
            val failure = AtResolver(at, it.clazz, it.method).isUnresolved()
            // leave it if there is a filter to blame, the target reference was at least resolved
            failure != null && failure.filterToBlame == null
        }
    }

    fun resolveNavigationTargets(context: PsiElement): Array<PsiElement>? {
        val at = context.parentOfType<PsiAnnotation>() ?: return null
        val targets = getTargets(at, false) ?: return null
        return targets.flatMap { AtResolver(at, it.clazz, it.method).resolveNavigationTargets() }.toTypedArray()
    }

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        val result = resolveTarget(context) ?: return ResolveResult.EMPTY_ARRAY
        return arrayOf(PsiElementResolveResult(result))
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val at = context.parentOfType<PsiAnnotation>() ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        val targets = getTargets(at, false) ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        return targets.flatMap { target ->
            AtResolver(at, target.clazz, target.method).collectTargetVariants { builder ->
                builder.completeToLiteral(context)
            }
        }.toTypedArray()
    }
}
