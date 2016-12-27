/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiAnnotationPattern
import com.intellij.patterns.PsiJavaElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElement

// Similar to PsiJavaElementPattern.insideAnnotationParam but with a
// PsiAnnotationPattern instead of only the qualified name
fun <T : PsiElement, Self : PsiJavaElementPattern<T, Self>> PsiJavaElementPattern<T, Self>
        .insideAnnotationParam(annotation: PsiAnnotationPattern, parameterName: String): Self {
    return withAncestor(3, PsiJavaPatterns.psiNameValuePair().withName(parameterName)
            .withParent(PlatformPatterns.psiElement(PsiAnnotationParameterList::class.java)
                    .withParent(annotation)))
}
