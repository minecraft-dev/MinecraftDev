/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DESC
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.util.parentOfType

object DescReference : AbstractMethodReference() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).insideAnnotationParam(
            StandardPatterns.string().equalTo(DESC)
        )

    override val description = "method '%s'"

    override fun isValidAnnotation(name: String) = name == DESC

    override fun parseSelector(context: PsiElement): MixinSelector? {
        val annotation = context.parentOfType<PsiAnnotation>() ?: return null // @Desc
        return DescSelectorParser.descSelectorFromAnnotation(annotation)
    }
}
