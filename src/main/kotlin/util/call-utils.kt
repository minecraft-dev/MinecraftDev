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

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression

val PsiCall.referencedMethod: PsiMethod?
    get() = when (this) {
        is PsiMethodCallExpression -> this.methodExpression.advancedResolve(false).element as PsiMethod?
        is PsiNewExpression -> this.resolveMethod()
        else -> null
    }

fun PsiCall.extractVarArgs(index: Int, allowReferences: Boolean, allowTranslations: Boolean): Array<String?> {
    val method = this.referencedMethod
    val args = this.argumentList?.expressions ?: return emptyArray()
    if (method == null || args.size < (index + 1)) {
        return emptyArray()
    }
    if (!method.parameterList.parameters[index].isVarArgs) {
        return arrayOf(args[index].evaluate(allowTranslations, allowReferences))
    }

    val varargType = method.getSignature(PsiSubstitutor.EMPTY).parameterTypes[index]
    val elements = args.drop(index)
    return extractVarArgs(varargType, elements, allowReferences, allowTranslations)
}

private fun extractVarArgs(
    type: PsiType,
    elements: List<PsiExpression>,
    allowReferences: Boolean,
    allowTranslations: Boolean
): Array<String?> {
    tailrec fun resolveReference(expression: PsiExpression): Array<String?> {
        if (expression is PsiTypeCastExpression && expression.operand != null) {
            return resolveReference(expression.operand!!)
        }
        return arrayOf(expression.evaluate(allowTranslations, allowReferences))
    }

    return if (elements[0].type == type) {
        // We're dealing with an array initializer, let's analyse it!
        val initializer = elements[0]
        if (initializer is PsiNewExpression && initializer.arrayInitializer != null) {
            initializer.arrayInitializer!!.initializers
                .asSequence()
                .map { it.evaluate(allowReferences, allowTranslations) }
                .toTypedArray()
        } else {
            resolveReference(initializer)
        }
    } else {
        elements.asSequence().map { it.evaluate(allowReferences, allowTranslations) }.toTypedArray()
    }
}
