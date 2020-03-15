/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

class MixinCompletionConfidence : CompletionConfidence() {

    private val mixinAnnotation = PlatformPatterns.psiElement()
        .inside(
            false, PsiJavaPatterns.psiAnnotation().qName(StandardPatterns.string().startsWith(MixinConstants.PACKAGE)),
            PlatformPatterns.psiFile()
        )!!

    override fun shouldSkipAutopopup(element: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        // Enable auto complete for all string literals which are children of one of the annotations in Mixin
        // TODO: Make this more reliable (we don't need to enable it for all parts of the annotation)
        return if (SkipAutopopupInStrings.isInStringLiteral(element) && mixinAnnotation.accepts(element)) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }
}
