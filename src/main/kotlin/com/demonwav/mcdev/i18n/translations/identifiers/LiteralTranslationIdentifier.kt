/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.translations.identifiers

import com.demonwav.mcdev.i18n.translations.Translation
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.PsiLiteralExpression

class LiteralTranslationIdentifier : TranslationIdentifier<PsiLiteralExpression>() {
    override fun identify(element: PsiLiteralExpression): Translation? {
        val statement = element.parent
        if (element.value is String) {
            val result = identify(element.project, element, statement, element)
            return result?.copy(
                key = result.key.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, ""),
                varKey = result.varKey.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
            )
        }
        return null
    }

    override fun elementClass(): Class<PsiLiteralExpression> = PsiLiteralExpression::class.java
}
