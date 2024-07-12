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
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastHintedVisitorAdapter
import com.intellij.uast.UastSmartPointer
import com.intellij.uast.createUastSmartPointer
import com.intellij.util.IncorrectOperationException
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class SuperfluousFormatInspection : TranslationInspection() {
    override fun getStaticDescription() = "Detect superfluous format arguments for translations"

    private val typesHint: Array<Class<out UElement>> =
        arrayOf(UReferenceExpression::class.java, ULiteralExpression::class.java)

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor =
        UastHintedVisitorAdapter.create(holder.file.language, Visitor(holder), typesHint)

    private class Visitor(private val holder: ProblemsHolder) : AbstractUastNonRecursiveVisitor() {

        override fun visitExpression(node: UExpression): Boolean {
            val result = TranslationInstance.find(node)
            if (
                result != null && result.foldingElement is UCallExpression &&
                result.formattingError == FormattingError.SUPERFLUOUS
            ) {
                registerProblem(node, result)
            }

            return super.visitExpression(node)
        }

        override fun visitLiteralExpression(node: ULiteralExpression): Boolean {
            val result = TranslationInstance.find(node)
            if (
                result != null && result.required && result.foldingElement is UCallExpression &&
                result.formattingError == FormattingError.SUPERFLUOUS
            ) {
                registerProblem(
                    node,
                    result,
                    RemoveArgumentsQuickFix(
                        result.foldingElement.createUastSmartPointer<UCallExpression>(),
                        result.superfluousVarargStart,
                    ),
                    ChangeTranslationQuickFix("Use a different translation"),
                )
            }

            return super.visitLiteralExpression(node)
        }

        private fun registerProblem(
            expression: UExpression,
            result: TranslationInstance,
            vararg quickFixes: LocalQuickFix,
        ) {
            holder.registerProblem(
                expression.sourcePsi!!,
                "There are missing formatting arguments to satisfy '${result.text}'",
                *quickFixes,
            )
        }
    }

    private class RemoveArgumentsQuickFix(
        private val call: UastSmartPointer<UCallExpression>,
        private val position: Int,
    ) : LocalQuickFix {
        override fun getName() = "Remove superfluous arguments"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                descriptor.psiElement.containingFile.runWriteAction {
                    call.element?.valueArguments?.drop(position)?.forEach { it.sourcePsi?.delete() }
                }
            } catch (ignored: IncorrectOperationException) {
            }
        }

        override fun startInWriteAction() = false

        override fun getFamilyName() = name
    }
}
