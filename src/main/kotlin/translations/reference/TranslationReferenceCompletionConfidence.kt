/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.reference

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState

class TranslationReferenceCompletionConfidence : CompletionConfidence() {
    override fun shouldSkipAutopopup(element: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        return if (
            SkipAutopopupInStrings.isInStringLiteral(element) &&
            element.parent.references.any { it is TranslationReference }
        ) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }
}
