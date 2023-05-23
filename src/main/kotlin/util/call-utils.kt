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
    allowTranslations: Boolean,
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
