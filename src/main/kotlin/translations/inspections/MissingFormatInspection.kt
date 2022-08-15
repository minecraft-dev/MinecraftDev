/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.inspections

import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.translations.identification.TranslationInstance.Companion.FormattingError
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
            val result = TranslationInstance.find(expression)
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
