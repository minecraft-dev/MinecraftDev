package com.demonwav.mcdev.platform.bukkit.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiClass

class BukkitEventHandlerCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inside(PsiClass::class.java),
            BukkitEventHandlerCompletionProvider()
        )
    }
}
