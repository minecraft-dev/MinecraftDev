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
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression

class MissingFormatInspection : TranslationInspection() {
    override fun getStaticDescription() = "Detects missing format arguments for translations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            visit(expression)
        }

        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            visit(expression, ChangeTranslationQuickFix("Use a different translation"))
        }

        private fun visit(expression: PsiExpression, vararg quickFixes: LocalQuickFix) {
            val result = Translation.find(expression)
            if (result != null && result.formattingError == FormattingError.MISSING) {
                holder.registerProblem(
                    expression,
                    "There are missing formatting arguments to satisfy '${result.text}'",
                    ProblemHighlightType.GENERIC_ERROR,
                    *quickFixes
                )
            }
        }
    }
}
