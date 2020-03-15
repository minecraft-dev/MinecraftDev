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

import com.intellij.patterns.ElementPattern
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
    attribute: String
): Self {
    return inside(
        true, PsiJavaPatterns.psiNameValuePair().withName(attribute)
            .withParent(
                PlatformPatterns.psiElement(PsiAnnotationParameterList::class.java)
                    .withParent(annotation)
            ), ANNOTATION_ATTRIBUTE_STOP
    )
}

fun <T : PsiElement, Self : PsiJavaElementPattern<T, Self>> PsiJavaElementPattern<T, Self>.insideAnnotationAttribute(
    annotation: ElementPattern<String>,
    attribute: String
): Self {
    return insideAnnotationAttribute(PsiJavaPatterns.psiAnnotation().qName(annotation), attribute)
}

fun <T : PsiElement, Self : PsiJavaElementPattern<T, Self>> PsiJavaElementPattern<T, Self>.insideAnnotationAttribute(
    annotation: String,
    attribute: String = "value"
): Self {
    return insideAnnotationAttribute(PsiJavaPatterns.psiAnnotation().qName(annotation), attribute)
}
