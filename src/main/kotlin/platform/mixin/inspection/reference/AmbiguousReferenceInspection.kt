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
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiBinaryExpression

class AmbiguousReferenceInspection : MixinAnnotationAttributeInspection("method") {

    override fun getStaticDescription() = "Reports ambiguous references in Mixin annotations"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder,
    ) {
        val handler = MixinAnnotationHandler.forMixinAnnotation(annotation)
        if (handler !is InjectorAnnotationHandler || handler.isSoft) {
            return
        }

        when (value) {
            is PsiArrayInitializerMemberValue -> value.initializers.forEach { checkMember(it, holder) }
            else -> checkMember(value, holder)
        }
    }

    private fun checkMember(value: PsiAnnotationMemberValue, holder: ProblemsHolder) {
        val ambiguousReference = MethodReference.getReferenceIfAmbiguous(value) ?: return
        if (ambiguousReference.matchAllNames || ambiguousReference.matchAllDescs) {
            // the intent of ambiguity is clear
            return
        }
        holder.registerProblem(
            value,
            "Ambiguous reference to method '${ambiguousReference.name}' in target class",
            QuickFix,
        )
    }

    private object QuickFix : LocalQuickFix {
        override fun getFamilyName() = "Add * wildcard"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement ?: return
            val constantValue = element.constantStringValue

            val elementFactory = JavaPsiFacade.getElementFactory(project)

            if (constantValue != null) {
                val newLiteral = "\"${StringUtil.escapeStringCharacters("$constantValue*")}\""
                element.replace(elementFactory.createExpressionFromText(newLiteral, null))
            } else {
                val replacement = elementFactory.createExpressionFromText("str + \"*\"", null) as PsiBinaryExpression
                replacement.lOperand.replace(element)
                element.replace(replacement)
            }
        }
    }
}
