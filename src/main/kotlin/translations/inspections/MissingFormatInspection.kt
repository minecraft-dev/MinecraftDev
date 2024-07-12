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
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastHintedVisitorAdapter
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class MissingFormatInspection : TranslationInspection() {
    override fun getStaticDescription() = "Detects missing format arguments for translations"

    private val typesHint: Array<Class<out UElement>> = arrayOf(UExpression::class.java)

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor =
        UastHintedVisitorAdapter.create(holder.file.language, Visitor(holder), typesHint)

    private class Visitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {

        override fun visitExpression(node: UExpression): Boolean {
            visit(node)
            return super.visitElement(node)
        }

        override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
            visit(node, ChangeTranslationQuickFix("Use a different translation"))
            return true
        }

        private fun visit(expression: UExpression, vararg quickFixes: LocalQuickFix) {
            val result = TranslationInstance.find(expression)
            if (result != null && result.required && result.formattingError == FormattingError.MISSING) {
                holder.registerProblem(
                    expression.sourcePsi!!,
                    "There are missing formatting arguments to satisfy '${result.text}'",
                    *quickFixes,
                )
            }
        }
    }
}
