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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.translations.identification.TranslationInstance
import com.demonwav.mcdev.translations.identification.TranslationInstance.Companion.FormattingError
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable

fun PsiAnnotationMemberValue.evaluate(allowReferences: Boolean, allowTranslations: Boolean): String? {
    val visited = mutableSetOf<PsiAnnotationMemberValue?>()

    fun eval(expr: PsiAnnotationMemberValue?, defaultValue: String? = null): String? {
        if (!visited.add(expr)) {
            return defaultValue
        }

        when {
            expr is PsiTypeCastExpression && expr.operand != null ->
                return eval(expr.operand, defaultValue)
            expr is PsiReferenceExpression -> {
                val reference = expr.advancedResolve(false).element
                if (reference is PsiVariable && reference.initializer != null) {
                    return eval(reference.initializer, "\${${expr.text}}")
                }
            }
            expr is PsiLiteral ->
                return expr.value.toString()
            expr is PsiCall && allowTranslations ->
                for (argument in expr.argumentList?.expressions ?: emptyArray()) {
                    val translation = TranslationInstance.find(argument) ?: continue
                    if (translation.formattingError == FormattingError.MISSING) {
                        return "{ERROR: Missing formatting arguments for '${translation.text}'}"
                    }

                    return translation.text
                }
        }

        return if (allowReferences && expr != null) {
            "\${${expr.text}}"
        } else {
            defaultValue
        }
    }

    return eval(this)
}
