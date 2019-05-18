/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.intellij.psi.PsiElement

data class TranslationInstance(
    val foldingElement: PsiElement?,
    val foldStart: Int,
    val referenceElement: PsiElement?,
    val key: Key,
    val text: String?,
    val formattingError: FormattingError? = null,
    val superfluousVarargStart: Int = -1
) {
    data class Key(val prefix: String, val infix: String, val suffix: String) {
        val full = (prefix + infix + suffix).trim()
    }

    companion object {
        enum class FormattingError {
            MISSING, SUPERFLUOUS
        }

        fun find(element: PsiElement): TranslationInstance? =
            TranslationIdentifier.INSTANCES
                .firstOrNull { it.elementClass().isAssignableFrom(element.javaClass) }
                ?.identifyUnsafe(element)
    }
}
