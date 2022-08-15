/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.util.MemberReference
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiLiteral

class UnnecessaryQualifiedMemberReferenceInspection : MixinAnnotationAttributeInspection("method") {

    override fun getStaticDescription() = "Reports unnecessary qualified member references in @Inject annotations"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val qName = annotation.qualifiedName ?: return
        if (MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project) !is InjectorAnnotationHandler) {
            return
        }

        when (value) {
            is PsiLiteral -> checkMemberReference(value, holder)
            is PsiArrayInitializerMemberValue -> value.initializers.forEach { checkMemberReference(it, holder) }
        }
    }

    private fun checkMemberReference(value: PsiAnnotationMemberValue, holder: ProblemsHolder) {
        val selector = parseMixinSelector(value) ?: return
        if (selector is MemberReference && selector.qualified) {
            holder.registerProblem(
                value,
                "Unnecessary qualified reference to '${selector.displayName}' in target class",
                QuickFix(selector)
            )
        }
    }

    private class QuickFix(private val reference: MemberReference) : LocalQuickFix {

        override fun getFamilyName() = "Remove qualifier"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            element.replace(
                JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText("\"${reference.withoutOwner.toMixinString()}\"", element)
            )
        }
    }
}
