/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.inspections

import com.demonwav.mcdev.i18n.translations.Translation
import com.demonwav.mcdev.i18n.translations.Translation.Companion.FormattingError
import com.demonwav.mcdev.util.runWriteAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
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
            val result = Translation.find(expression)
            if (
                result != null && result.foldingElement is PsiCall &&
                result.formattingError == FormattingError.SUPERFLUOUS
            ) {
                registerProblem(expression, result)
            }
        }

        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            val result = Translation.find(expression)
            if (
                result != null && result.foldingElement is PsiCall &&
                result.formattingError == FormattingError.SUPERFLUOUS
            ) {
                registerProblem(
                    expression,
                    result,
                    RemoveArgumentsQuickFix(
                        SmartPointerManager.getInstance(holder.project)
                            .createSmartPsiElementPointer(result.foldingElement),
                        result.superfluousVarargStart
                    ),
                    ChangeTranslationQuickFix("Use a different translation")
                )
            }
        }

        private fun registerProblem(expression: PsiExpression, result: Translation, vararg quickFixes: LocalQuickFix) {
            holder.registerProblem(
                expression,
                "There are missing formatting arguments to satisfy '${result.text}'",
                ProblemHighlightType.GENERIC_ERROR,
                *quickFixes
            )
        }
    }

    private class RemoveArgumentsQuickFix(
        private val call: SmartPsiElementPointer<PsiCall>,
        private val position: Int
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
