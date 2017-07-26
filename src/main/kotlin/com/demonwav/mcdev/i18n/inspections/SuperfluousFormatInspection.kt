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

import com.demonwav.mcdev.i18n.translations.identifiers.LiteralTranslationIdentifier
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import org.jetbrains.annotations.Nls

class SuperfluousFormatInspection : TranslationInspection() {
    @Nls
    override fun getDisplayName() = "Detect superfluous format arguments for translations"

    override fun checkElement(element: PsiElement?, holder: ProblemsHolder) {
        if (element is PsiLiteralExpression) {
            val result = LiteralTranslationIdentifier().identify(element)
            if (result != null && result.formattingError) {
                holder.registerProblem(element, "There are missing formatting arguments to satisfy '${result.text}'", ProblemHighlightType.GENERIC_ERROR, ChangeTranslationQuickFix("Use a different translation"))
            }
        }
    }
}
