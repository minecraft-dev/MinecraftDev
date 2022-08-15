/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.psi.PsiLiteralExpression

class LiteralTranslationIdentifier : TranslationIdentifier<PsiLiteralExpression>() {
    override fun identify(element: PsiLiteralExpression): TranslationInstance? {
        val statement = element.parent
        if (element.value is String) {
            val result = identify(element.project, element, statement, element)
            return result?.copy(
                key = result.key.copy(
                    infix = result.key.infix.replace(
                        CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED,
                        ""
                    )
                )
            )
        }
        return null
    }

    override fun elementClass(): Class<PsiLiteralExpression> = PsiLiteralExpression::class.java
}
