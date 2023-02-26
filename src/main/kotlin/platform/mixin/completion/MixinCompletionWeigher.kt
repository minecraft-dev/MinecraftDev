/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.CompletionWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.PsiClass

class MixinCompletionWeigher : CompletionWeigher() {
    private val mixinAnnotation = PlatformPatterns.psiElement()
        .inside(
            false,
            PsiJavaPatterns.psiAnnotation().qName(MixinConstants.Annotations.MIXIN),
            PlatformPatterns.psiFile()
        )!!

    override fun weigh(element: LookupElement, location: CompletionLocation): Int {
        val lookupClass = element.psiElement as? PsiClass ?: return 0

        val position = location.completionParameters.position
        if (!mixinAnnotation.accepts(position)) {
            return 0
        }

        val clazz = position.findContainingClass() ?: return 0
        if (clazz equivalentTo lookupClass) {
            return -1
        }

        return 0
    }
}
