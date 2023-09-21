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
                        "",
                    ),
                ),
            )
        }
        return null
    }

    override fun elementClass(): Class<PsiLiteralExpression> = PsiLiteralExpression::class.java
}
