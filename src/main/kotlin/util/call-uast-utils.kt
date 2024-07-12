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

import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.util.isArrayInitializer

val UCallExpression.referencedMethod: UMethod?
    get() = this.resolve()?.toUElementOfType<UMethod>()

fun UCallExpression.extractVarArgs(index: Int, allowReferences: Boolean, allowTranslations: Boolean): Array<String?>? {
    val method = this.referencedMethod
    val args = this.valueArguments
    if (method == null || args.size < (index + 1)) {
        return emptyArray()
    }

    val psiParam = method.uastParameters[index].javaPsi as? PsiParameter
        ?: return null
    if (!psiParam.isVarArgs) {
        return arrayOf(args[index].evaluate(allowTranslations, allowReferences))
    }

    val elements = args.drop(index)
    return extractVarArgs(psiParam.type, elements, allowReferences, allowTranslations)
}

private fun extractVarArgs(
    type: PsiType,
    elements: List<UExpression>,
    allowReferences: Boolean,
    allowTranslations: Boolean,
): Array<String?>? {
    return if (elements[0].getExpressionType() == type) {
        val initializer = elements[0]
        if (initializer is UCallExpression && initializer.isArrayInitializer()) {
            // We're dealing with an array initializer, let's analyse it!
            initializer.valueArguments
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
