/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
    val superfluousVarargStart: Int = -1,
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
                    TranslationConstants.I18N_CLIENT_CLASS,
                ),
                0,
                formatting = true,
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.GET,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    TranslationConstants.I18N_CLIENT_LANG_CLASS,
                ),
                0,
                formatting = true,
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.EXISTS,
                    "(Ljava/lang/String;)Z",
                    TranslationConstants.I18N_CLIENT_LANG_CLASS,
                ),
                0,
                formatting = false,
                obfuscatedName = true,
                foldParameters = TranslationFunction.FoldingScope.PARAMETERS,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.TRANSLATE_TO_LOCAL,
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    TranslationConstants.I18N_COMMON_CLASS,
                ),
                0,
                formatting = false,
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.TRANSLATE_TO_LOCAL_FORMATTED,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    TranslationConstants.I18N_COMMON_CLASS,
                ),
                0,
                formatting = true,
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    TranslationConstants.TRANSLATION_COMPONENT_CLASS,
                ),
                0,
                formatting = true,
                foldParameters = TranslationFunction.FoldingScope.PARAMETERS,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;)V",
                    TranslationConstants.TRANSLATABLE_COMPONENT,
                ),
                0,
                formatting = false,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    TranslationConstants.TRANSLATABLE_COMPONENT,
                ),
                0,
                formatting = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CREATE_COMPONENT_TRANSLATION,
                    "(Lnet/minecraft/commands/CommandSource;Ljava/lang/String;" +
                        "[Ljava/lang/Object;)Lnet/minecraft/network/chat/BaseComponent;",
                    TranslationConstants.TEXT_COMPONENT_HELPER,
                ),
                1,
                formatting = true,
                foldParameters = TranslationFunction.FoldingScope.PARAMETERS,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    TranslationConstants.COMMAND_EXCEPTION_CLASS,
                ),
                0,
                formatting = true,
                foldParameters = TranslationFunction.FoldingScope.PARAMETERS,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.SET_BLOCK_NAME,
                    "(Ljava/lang/String;)Lnet/minecraft/block/Block;",
                    McpConstants.BLOCK,
                ),
                0,
                formatting = false,
                setter = true,
                foldParameters = TranslationFunction.FoldingScope.PARAMETERS,
                prefix = "tile.",
                suffix = ".name",
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.SET_ITEM_NAME,
                    "(Ljava/lang/String;)Lnet/minecraft/item/Item;",
                    McpConstants.ITEM,
                ),
                0,
                formatting = false,
                setter = true,
                foldParameters = TranslationFunction.FoldingScope.PARAMETERS,
                prefix = "item.",
                suffix = ".name",
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                2,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants\$Type;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants\$Type;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                3,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Type;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Type;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                4,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Key;Ljava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Key;Ljava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                3,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lnet/minecraftforge/client/settings/KeyModifier;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Type;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lnet/minecraftforge/client/settings/KeyModifier;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Type;ILjava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                5,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lnet/minecraftforge/client/settings/KeyModifier;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Key;Ljava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;Lnet/minecraftforge/client/settings/IKeyConflictContext;" +
                        "Lnet/minecraftforge/client/settings/KeyModifier;" +
                        "Lcom/mojang/blaze3d/platform/InputConstants\$Key;Ljava/lang/String;)V",
                    TranslationConstants.KEY_MAPPING,
                ),
                4,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.INPUT_CONSTANTS_KEY_GET_KEY,
                    "(Ljava/lang/String;)Lcom/mojang/blaze3d/platform/InputConstants\$Key;",
                    TranslationConstants.INPUT_CONSTANTS_KEY,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
                obfuscatedName = true,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.COMPONENT_TRANSLATABLE,
                    "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                    TranslationConstants.COMPONENT_CLASS,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.CALL,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.COMPONENT_TRANSLATABLE,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
                    TranslationConstants.COMPONENT_CLASS,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.CALL,
            ),
            TranslationFunction(
                MemberReference(
                    TranslationConstants.COMPONENT_TRANSLATABLE_WITH_FALLBACK,
                    null,
                    TranslationConstants.COMPONENT_CLASS,
                ),
                0,
                formatting = false,
                foldParameters = TranslationFunction.FoldingScope.PARAMETER,
            ),
        )

        fun find(element: PsiElement): TranslationInstance? =
            TranslationIdentifier.INSTANCES
                .firstOrNull { it.elementClass().isAssignableFrom(element.javaClass) }
                ?.identifyUnsafe(element)
    }
}
