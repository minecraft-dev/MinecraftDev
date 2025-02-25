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

class UnnecessaryQualifiedMemberReferenceInspection : MixinAnnotationAttributeInspection("method") {

    override fun getStaticDescription() = "Reports unnecessary qualified member references in @Inject annotations"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder,
    ) {
        if (MixinAnnotationHandler.forMixinAnnotation(annotation) !is InjectorAnnotationHandler) {
            return
        }

        when (value) {
            is PsiArrayInitializerMemberValue -> value.initializers.forEach { checkMemberReference(it, holder) }
            else -> checkMemberReference(value, holder)
        }
    }

    private fun checkMemberReference(value: PsiAnnotationMemberValue, holder: ProblemsHolder) {
        val selector = parseMixinSelector(value) ?: return
        if (selector is MemberReference && selector.qualified) {
            holder.registerProblem(
                value,
                "Unnecessary qualified reference to '${selector.displayName}' in target class",
                QuickFix(selector),
            )
        }
    }

    private class QuickFix(private val reference: MemberReference) : LocalQuickFix {

        override fun getFamilyName() = "Remove qualifier"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            element.replace(
                JavaPsiFacade.getElementFactory(project)
                    .createExpressionFromText("\"${reference.withoutOwner.toMixinString()}\"", element),
            )
        }
    }
}
