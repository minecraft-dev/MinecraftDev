/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.completion

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.codeInsight.completion.SkipAutopopupInStrings
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import com.intellij.util.ThreeState

class SpongeCompletionConfidence : CompletionConfidence() {

    override fun shouldSkipAutopopup(element: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        if (!SkipAutopopupInStrings.isInStringLiteral(element)) {
            return ThreeState.UNSURE
        }

        val memberValue = element.parentOfType(PsiAnnotationMemberValue::class) ?: return ThreeState.UNSURE
        val annotation = memberValue.parentOfType(PsiAnnotation::class) ?: return ThreeState.UNSURE

        val method = element.findContainingMethod() ?: return ThreeState.UNSURE
        return if (
            method.hasAnnotation(SpongeConstants.LISTENER_ANNOTATION) &&
            annotation.qualifiedName == SpongeConstants.GETTER_ANNOTATION
        ) {
            ThreeState.NO
        } else {
            ThreeState.UNSURE
        }
    }
}
