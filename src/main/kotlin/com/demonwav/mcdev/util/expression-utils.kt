/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.i18n.translations.Translation
import com.demonwav.mcdev.i18n.translations.Translation.Companion.FormattingError
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable

fun PsiAnnotationMemberValue.evaluate(defaultValue: String?, parameterReplacement: String?): String? {
    val visited = mutableSetOf<PsiAnnotationMemberValue?>()

    fun eval(expr: PsiAnnotationMemberValue?, defaultValue: String?): String? {
        if (!visited.add(expr)) {
            return defaultValue
        }
        when {
            expr is PsiTypeCastExpression && expr.operand != null ->
                return eval(expr.operand, defaultValue)
            expr is PsiReferenceExpression -> {
                val reference = expr.advancedResolve(false).element
                if (reference is PsiParameter) {
                    return parameterReplacement
                }
                if (reference is PsiVariable && reference.initializer != null) {
                    return eval(reference.initializer, null)
                }
            }
            expr is PsiLiteral ->
                return expr.value.toString()
            expr is PsiPolyadicExpression && expr.operationTokenType == JavaTokenType.PLUS -> {
                var value = ""
                for (operand in expr.operands) {
                    val operandResult = eval(operand, defaultValue) ?: return defaultValue
                    value += operandResult
                }
                return value
            }
        }

        return defaultValue
    }

    return eval(this, defaultValue)
}

fun PsiExpression.substituteParameter(
    substitutions: Map<Int, Array<String?>?>,
    allowReferences: Boolean,
    allowTranslations: Boolean
): Array<String?>? {
    val visited = mutableSetOf<PsiExpression?>()
    fun substitute(expr: PsiExpression?): Array<String?>? {
        if (!visited.add(expr) && expr != null) {
            return arrayOf("\${${expr.text}}")
        }
        when {
            expr is PsiTypeCastExpression && expr.operand != null ->
                return substitute(expr.operand)
            expr is PsiReferenceExpression -> {
                val reference = expr.advancedResolve(false).element
                if (reference is PsiParameter && reference.parent is PsiParameterList) {
                    val paramIndex = (reference.parent as PsiParameterList).getParameterIndex(reference)
                    if (substitutions.containsKey(paramIndex)) {
                        return substitutions[paramIndex]
                    }
                }
                if (reference is PsiVariable && reference.initializer != null) {
                    return substitute(reference.initializer)
                }
            }
            expr is PsiLiteral ->
                return arrayOf(expr.value.toString())
            expr is PsiPolyadicExpression && expr.operationTokenType == JavaTokenType.PLUS -> {
                var value = ""
                for (operand in expr.operands) {
                    val operandResult = operand.evaluate(null, null) ?: return null
                    value += operandResult
                }
                return arrayOf(value)
            }
            expr is PsiCall && allowTranslations ->
                for (argument in expr.argumentList?.expressions ?: emptyArray()) {
                    val translation = Translation.find(argument) ?: continue
                    if (translation.formattingError == FormattingError.MISSING) {
                        return arrayOf("{ERROR: Missing formatting arguments for '${translation.text}'}")
                    }
                    return arrayOf(translation.text)
                }
        }
        return if (allowReferences && expr != null) {
            arrayOf("\${${expr.text}}")
        } else {
            null
        }
    }
    return substitute(this)
}
