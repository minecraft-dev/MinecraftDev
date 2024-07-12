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

package com.demonwav.mcdev.translations.identification

import com.intellij.codeInsight.completion.CompletionUtilCore
import org.jetbrains.uast.ULiteralExpression

class LiteralTranslationIdentifier : TranslationIdentifier<ULiteralExpression>() {
    override fun identify(element: ULiteralExpression): TranslationInstance? {
        val statement = element.uastParent ?: return null
        if (element.value !is String) {
            return null
        }

        val project = element.sourcePsi?.project ?: return null
        val result = identify(project, element, statement, element) ?: return null
        val infix = result.key.infix.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
        return result.copy(key = result.key.copy(infix = infix))
    }

    override fun elementClass(): Class<ULiteralExpression> = ULiteralExpression::class.java
}
