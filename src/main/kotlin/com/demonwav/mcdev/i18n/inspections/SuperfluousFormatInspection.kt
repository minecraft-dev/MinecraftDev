/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nls

class SuperfluousFormatInspection : TranslationInspection() {
    @Nls
    override fun getDisplayName() = "Detect superfluous format arguments for translations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            val result = Translation.find(expression)
            if (result != null && result.foldingElement is PsiCall && result.formattingError == FormattingError.SUPERFLUOUS) {
                registerProblem(expression, result)
            }
        }

        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            val result = Translation.find(expression)
            if (result != null && result.foldingElement is PsiCall && result.formattingError == FormattingError.SUPERFLUOUS) {
                registerProblem(expression, result, RemoveArgumentsQuickFix(result.foldingElement, result.superfluousVarargStart), ChangeTranslationQuickFix("Use a different translation"))
            }
        }

        private fun registerProblem(expression: PsiExpression, result: Translation, vararg quickFixes: LocalQuickFix) {
            holder.registerProblem(expression, "There are missing formatting arguments to satisfy '${result.text}'", ProblemHighlightType.GENERIC_ERROR, *quickFixes)
        }
    }

    companion object {
        private class RemoveArgumentsQuickFix(private val call: PsiCall, private val position: Int) : LocalQuickFix {
            override fun getName() = "Remove superfluous arguments"

            override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
                try {
                    descriptor.psiElement.containingFile.runWriteAction {
                        call.argumentList?.expressions?.drop(position)?.forEach { it.delete() }
                    }
                } catch (ignored: IncorrectOperationException) {
                }
            }

            override fun startInWriteAction() = false

            override fun getFamilyName() = name
        }
    }
}
