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
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import org.jetbrains.annotations.Nls

class MissingFormatInspection : TranslationInspection() {
    @Nls
    override fun getDisplayName() = "Detect missing format arguments for translations"

    override fun checkElement(element: PsiElement, holder: ProblemsHolder) {
        val result = Translation.find(element)
        if (result != null && result.formattingError == FormattingError.MISSING) {
            val quickFixes = if (element is PsiLiteralExpression) arrayOf(ChangeTranslationQuickFix("Use a different translation")) else emptyArray()
            holder.registerProblem(element, "There are missing formatting arguments to satisfy '${result.text}'", ProblemHighlightType.GENERIC_ERROR, *quickFixes)
        }
    }
}
