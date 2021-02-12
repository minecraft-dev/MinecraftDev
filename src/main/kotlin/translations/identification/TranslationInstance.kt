/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.translations.TranslationConstants
import com.demonwav.mcdev.util.MemberReference
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

        val translationFunctions = listOf(
            TranslationFunction(
                MemberReference(
                    TranslationConstants.FORMAT,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    TranslationConstants.I18N_CLIENT_CLASS
                ),
                0,
                formatting = true,
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.TRANSLATE_TO_LOCAL,
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    TranslationConstants.I18N_COMMON_CLASS
                ),
                0,
                formatting = false,
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.TRANSLATE_TO_LOCAL_FORMATTED,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    TranslationConstants.I18N_COMMON_CLASS
                ),
                0,
                formatting = true,
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    TranslationConstants.TRANSLATION_COMPONENT_CLASS
                ),
                0,
                formatting = true,
                foldParameters = true
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    TranslationConstants.COMMAND_EXCEPTION_CLASS
                ),
                0,
                formatting = true,
                foldParameters = true
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.SET_BLOCK_NAME,
                    "(Ljava/lang/String;)Lnet/minecraft/block/Block;",
                    McpConstants.BLOCK
                ),
                0,
                formatting = false,
                setter = true,
                foldParameters = true,
                prefix = "tile.",
                suffix = ".name",
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.SET_ITEM_NAME,
                    "(Ljava/lang/String;)Lnet/minecraft/item/Item;",
                    McpConstants.ITEM
                ),
                0,
                formatting = false,
                setter = true,
                foldParameters = true,
                prefix = "item.",
                suffix = ".name",
                obfuscatedName = true
            )
        )

        fun find(element: PsiElement): TranslationInstance? =
            TranslationIdentifier.INSTANCES
                .firstOrNull { it.elementClass().isAssignableFrom(element.javaClass) }
                ?.identifyUnsafe(element)
    }
}
