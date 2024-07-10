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

import com.intellij.psi.PsiCall
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType

val PsiCall.referencedMethod: PsiMethod?
    get() = when (this) {
        is PsiMethodCallExpression -> this.methodExpression.advancedResolve(false).element as PsiMethod?
        is PsiNewExpression -> this.resolveMethod()
        is PsiEnumConstant -> this.resolveMethod()
        else -> null
    }

fun PsiCall.extractVarArgs(index: Int, allowReferences: Boolean, allowTranslations: Boolean): Array<String?>? {
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
): Array<String?>? {
    return if (elements[0].type == type) {
        val initializer = elements[0]
        if (initializer is PsiNewExpression && initializer.arrayInitializer != null) {
            // We're dealing with an array initializer, let's analyse it!
            initializer.arrayInitializer!!.initializers
                .asSequence()
                .map { it.evaluate(allowReferences, allowTranslations) }
                .toTypedArray()
        } else {
            // We're dealing with a more complex expression that results in an array, give up
            return null
        }
    } else {
        elements.asSequence().map { it.evaluate(allowReferences, allowTranslations) }.toTypedArray()
    }
}
