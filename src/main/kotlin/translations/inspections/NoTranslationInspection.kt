/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.identification.LiteralTranslationIdentifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.util.IncorrectOperationException

class NoTranslationInspection : TranslationInspection() {
    override fun getStaticDescription() =
        "Checks whether a translation key used in calls to <code>StatCollector.translateToLocal()</code>, " +
            "<code>StatCollector.translateToLocalFormatted()</code> or <code>I18n.format()</code> exists."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitLiteralExpression(expression: PsiLiteralExpression) {
            val result = LiteralTranslationIdentifier().identify(expression)
            if (result != null && result.required && result.text == null) {
                holder.registerProblem(
                    expression,
                    "The given translation key does not exist",
                    CreateTranslationQuickFix,
                    ChangeTranslationQuickFix("Use existing translation"),
                )
            }
        }
    }

    private object CreateTranslationQuickFix : LocalQuickFix {
        override fun getName() = "Create translation"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            try {
                val literal = descriptor.psiElement as PsiLiteralExpression
                val translation = LiteralTranslationIdentifier().identify(literal)
                val literalValue = literal.value as String
                val key = translation?.key?.copy(infix = literalValue)?.full ?: literalValue
                val result = Messages.showInputDialog(
                    "Enter default value for \"$key\":",
                    "Create Translation",
                    Messages.getQuestionIcon(),
                )
                if (result != null) {
                    TranslationFiles.add(literal, key, result)
                }
            } catch (ignored: IncorrectOperationException) {
            } catch (e: Exception) {
                Notification(
                    "Translation support error",
                    "Error while adding translation",
                    e.message ?: e.stackTraceToString(),
                    NotificationType.WARNING,
                ).notify(project)
            }
        }

        override fun startInWriteAction() = false

        override fun getFamilyName() = name
    }
}
