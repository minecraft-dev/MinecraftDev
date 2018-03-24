/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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
    if (this is PsiTypeCastExpression && this.operand != null) {
        return this.operand!!.evaluate(defaultValue, parameterReplacement)
    }
    if (this is PsiReferenceExpression) {
        val reference = this.advancedResolve(false).element
        if (reference is PsiParameter) {
            return parameterReplacement
        }
        if (reference is PsiVariable && reference.initializer != null) {
            return reference.initializer!!.evaluate(null, parameterReplacement)
        }
    } else if (this is PsiLiteral) {
        return this.value.toString()
    } else if (this is PsiPolyadicExpression) {
        var value = ""
        for (operand in this.operands) {
            val operandResult = operand.evaluate(defaultValue, parameterReplacement) ?: return defaultValue
            when (this.operationTokenType) {
                JavaTokenType.PLUS -> value += operandResult
            }
        }
        return value
    }
    return defaultValue
}

fun PsiExpression.substituteParameter(substitutions: Map<Int, Array<String?>?>, allowReferences: Boolean, allowTranslations: Boolean): Array<String?>? {
    if (this is PsiTypeCastExpression && this.operand != null) {
        return this.operand!!.substituteParameter(substitutions, allowReferences, allowTranslations)
    }
    if (this is PsiReferenceExpression) {
        val reference = this.advancedResolve(false).element
        if (reference is PsiParameter && reference.parent is PsiParameterList) {
            val paramIndex = (reference.parent as PsiParameterList).getParameterIndex(reference)
            if (substitutions.containsKey(paramIndex)) {
                return substitutions[paramIndex]
            }
        }
        if (reference is PsiVariable && reference.initializer != null) {
            return reference.initializer!!.substituteParameter(substitutions, allowReferences, allowTranslations)
        }
    } else if (this is PsiLiteral) {
        return arrayOf(this.value.toString())
    } else if (this is PsiPolyadicExpression) {
        var value = ""
        for (operand in this.operands) {
            val operandResult = operand.evaluate(null, null) ?: return null
            when (this.operationTokenType) {
                JavaTokenType.PLUS -> value += operandResult
            }
        }
        return arrayOf(value)
    } else if (this is PsiCall && allowTranslations) {
        for (argument in this.argumentList?.expressions ?: emptyArray()) {
            val translation = Translation.find(argument) ?: continue
            if (translation.formattingError == FormattingError.MISSING) {
                return arrayOf("{ERROR: Missing formatting arguments for '${translation.text}'}")
            }
            return arrayOf(translation.text)
        }
    }
    return if (allowReferences) {
        arrayOf("\${${this.text}}")
    } else {
        null
    }
}
