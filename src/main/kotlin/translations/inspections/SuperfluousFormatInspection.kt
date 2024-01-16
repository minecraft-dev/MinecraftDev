/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.translations.inspections

import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.translations.identification.TranslationInstance.Companion.FormattingError
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.IncorrectOperationException

class SuperfluousFormatInspection : TranslationInspection() {
    override fun getStaticDescription() = "Detect superfluous format arguments for translations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            val result = TranslationInstance.find(expression)
            if (
                result != null && result.foldingElement is PsiCall &&
                result.formattingError == FormattingError.SUPERFLUOUS
            ) {
                registerProblem(expression, result)
            }
        }

        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            val result = TranslationInstance.find(expression)
            if (
                result != null && result.required && result.foldingElement is PsiCall &&
                result.formattingError == FormattingError.SUPERFLUOUS
            ) {
                registerProblem(
                    expression,
                    result,
                    RemoveArgumentsQuickFix(
                        SmartPointerManager.getInstance(holder.project)
                            .createSmartPsiElementPointer(result.foldingElement),
                        result.superfluousVarargStart,
                    ),
                    ChangeTranslationQuickFix("Use a different translation"),
                )
            }
        }

        private fun registerProblem(
            expression: PsiExpression,
            result: TranslationInstance,
            vararg quickFixes: LocalQuickFix,
        ) {
            holder.registerProblem(
                expression,
                "There are missing formatting arguments to satisfy '${result.text}'",
                *quickFixes,
            )
        }
    }

    private class RemoveArgumentsQuickFix(
        private val call: SmartPsiElementPointer<PsiCall>,
        private val position: Int,
    ) : LocalQuickFix {
        override fun getName() = "Remove superfluous arguments"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                descriptor.psiElement.containingFile.runWriteAction {
                    call.element?.argumentList?.expressions?.drop(position)?.forEach { it.delete() }
                }
            } catch (ignored: IncorrectOperationException) {
            }
        }

        override fun startInWriteAction() = false

        override fun getFamilyName() = name
    }
}
