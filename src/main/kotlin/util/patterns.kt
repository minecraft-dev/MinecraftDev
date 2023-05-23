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

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiAnnotationPattern
import com.intellij.patterns.PsiJavaElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElement

private val ANNOTATION_ATTRIBUTE_STOP =
    PlatformPatterns.not(PsiJavaPatterns.psiExpression()).andNot(PsiJavaPatterns.psiNameValuePair())

// PsiJavaElementPattern.insideAnnotationParam checks for the parameter list only up to 3 levels
// It can be more if the value is for example enclosed in parentheses
fun <T : PsiElement, Self : PsiJavaElementPattern<T, Self>> PsiJavaElementPattern<T, Self>.insideAnnotationAttribute(
    annotation: PsiAnnotationPattern,
    attribute: String,
): Self {
    return inside(
        true,
        PsiJavaPatterns.psiNameValuePair().withName(attribute)
            .withParent(
                PlatformPatterns.psiElement(PsiAnnotationParameterList::class.java)
                    .withParent(annotation),
            ),
        ANNOTATION_ATTRIBUTE_STOP,
    )
}

fun <T : PsiElement, Self : PsiJavaElementPattern<T, Self>> PsiJavaElementPattern<T, Self>.insideAnnotationAttribute(
    annotation: String,
    attribute: String = "value",
): Self {
    return insideAnnotationAttribute(PsiJavaPatterns.psiAnnotation().qName(annotation), attribute)
}
