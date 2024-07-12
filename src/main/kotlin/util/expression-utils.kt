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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.translations.identification.TranslationInstance.Companion.FormattingError
import org.jetbrains.uast.UBinaryExpressionWithType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.resolveToUElement
import org.jetbrains.uast.util.isTypeCast

fun UExpression.evaluate(allowReferences: Boolean, allowTranslations: Boolean): String? {
    val visited = mutableSetOf<UExpression?>()

    fun eval(expr: UExpression?, defaultValue: String? = null): String? {
        if (!visited.add(expr)) {
            return defaultValue
        }

        when {
            expr is UBinaryExpressionWithType && expr.isTypeCast() ->
                return eval(expr.operand, defaultValue)

            expr is UQualifiedReferenceExpression -> {
                val selector = expr.selector
                if (selector is UCallExpression) {
                    return eval(selector, "\${${expr.asSourceString()}}")
                }
            }

            expr is UReferenceExpression -> {
                val reference = expr.resolveToUElement()
                if (reference is UVariable && reference.uastInitializer != null) {
                    return eval(reference.uastInitializer, "\${${expr.asSourceString()}}")
                }
            }

            expr is UCallExpression && allowTranslations ->
                for (argument in expr.valueArguments) {
                    val translation = TranslationInstance.find(argument) ?: continue
                    if (translation.formattingError == FormattingError.MISSING) {
                        return "{ERROR: Missing formatting arguments for '${translation.text}'}"
                    }

                    return translation.text
                }

            else -> expr?.evaluateString()?.let { return it }
        }

        return if (allowReferences && expr != null) {
            "\${${expr.asSourceString()}}"
        } else {
            defaultValue
        }
    }

    return eval(this)
}
