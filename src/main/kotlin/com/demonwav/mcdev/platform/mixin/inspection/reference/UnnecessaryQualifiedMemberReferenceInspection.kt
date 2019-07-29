/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.METHOD_INJECTORS
import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class UnnecessaryQualifiedMemberReferenceInspection : MixinAnnotationAttributeInspection(METHOD_INJECTORS, "method") {

    override fun getStaticDescription() = "Reports unnecessary qualified member references in @Inject annotations"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val reference = MixinMemberReference.parse(value.constantStringValue) ?: return
        if (reference.qualified) {
            holder.registerProblem(
                value,
                "Unnecessary qualified reference to '${reference.name}' in target class",
                QuickFix(reference)
            )
        }
    }

    private class QuickFix(private val reference: MemberReference) : LocalQuickFix {

        override fun getFamilyName() = "Remove qualifier"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            element.replace(
                JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText("\"${MixinMemberReference.toString(reference.withoutOwner)}\"", element)
            )
        }
    }
}
