/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
