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
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.Nls

class SuperfluousFormatInspection : TranslationInspection() {
    @Nls
    override fun getDisplayName() = "Detect superfluous format arguments for translations"

    override fun checkElement(element: PsiElement, holder: ProblemsHolder) {
        val result = Translation.find(element)
        if (result != null && result.foldingElement is PsiCall && result.formattingError == FormattingError.SUPERFLUOUS) {
            val quickFixes = if (element is PsiLiteralExpression) arrayOf(RemoveArgumentsQuickFix(result.foldingElement, result.superfluousVarargStart), ChangeTranslationQuickFix("Use a different translation")) else emptyArray()
            holder.registerProblem(element, "There are too many formatting arguments for '${result.text}'", ProblemHighlightType.WEAK_WARNING, *quickFixes)
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
