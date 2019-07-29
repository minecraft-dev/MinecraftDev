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

import com.demonwav.mcdev.i18n.I18nElementFactory
import com.demonwav.mcdev.i18n.reference.I18nReference
import com.demonwav.mcdev.i18n.translations.identifiers.LiteralTranslationIdentifier
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
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
            if (result != null && !result.containsVariable && result.text == null) {
                holder.registerProblem(
                    expression,
                    "The given translation key does not exist",
                    ProblemHighlightType.GENERIC_ERROR,
                    CreateTranslationQuickFix, ChangeTranslationQuickFix("Use existing translation")
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
                val key = translation?.varKey?.replace(I18nReference.VARIABLE_MARKER, literalValue) ?: literalValue
                val result = Messages.showInputDialog(
                    "Enter default value for \"$key\":",
                    "Create Translation",
                    Messages.getQuestionIcon()
                )
                if (result != null) {
                    I18nElementFactory.addTranslation(
                        ProjectRootManager.getInstance(project).fileIndex
                            .getModuleForFile(literal.containingFile.virtualFile),
                        key,
                        result
                    )
                }
            } catch (ignored: IncorrectOperationException) {
            }
        }

        override fun startInWriteAction() = false

        override fun getFamilyName() = name
    }
}
