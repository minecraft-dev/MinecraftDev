/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.translations

import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.reference.I18nReference
import com.demonwav.mcdev.i18n.translations.identifiers.LiteralTranslationIdentifier
import com.demonwav.mcdev.i18n.translations.identifiers.ReferenceTranslationIdentifier
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.util.MemberReference
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

data class Translation(
    val foldingElement: PsiElement?,
    val referenceElement: PsiElement?,
    val key: String,
    val varKey: String,
    val text: String?,
    val formattingError: FormattingError? = null,
    val superfluousVarargStart: Int = -1,
    val containsVariable: Boolean = false
) {
    val regexPattern = Regex(varKey.split(I18nReference.VARIABLE_MARKER).joinToString("(.*?)") { Regex.escape(it) })

    companion object {
        enum class FormattingError {
            MISSING, SUPERFLUOUS
        }

        val translationFunctions = listOf(
            TranslationFunction(
                MemberReference(
                    I18nConstants.FORMAT,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    I18nConstants.I18N_CLIENT_CLASS
                ),
                0,
                formatting = true,
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    I18nConstants.TRANSLATE_TO_LOCAL,
                    "(Ljava/lang/String;)Ljava/lang/String;",
                    I18nConstants.I18N_COMMON_CLASS
                ),
                0,
                formatting = false,
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    I18nConstants.TRANSLATE_TO_LOCAL_FORMATTED,
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
                    I18nConstants.I18N_COMMON_CLASS
                ),
                0,
                formatting = true,
                obfuscatedName = true
            ),
            TranslationFunction(
                MemberReference(
                    I18nConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    I18nConstants.TRANSLATION_COMPONENT_CLASS
                ),
                0,
                formatting = true,
                foldParameters = true
            ),
            TranslationFunction(
                MemberReference(
                    I18nConstants.CONSTRUCTOR,
                    "(Ljava/lang/String;[Ljava/lang/Object;)V",
                    I18nConstants.COMMAND_EXCEPTION_CLASS
                ),
                0,
                formatting = true,
                foldParameters = true
            ),
            TranslationFunction(
                MemberReference(
                    I18nConstants.SET_BLOCK_NAME,
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
                    I18nConstants.SET_ITEM_NAME,
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

        private val identifiers = listOf(LiteralTranslationIdentifier(), ReferenceTranslationIdentifier())

        fun find(element: PsiElement): Translation? =
            identifiers.firstOrNull { it.elementClass().isAssignableFrom(element.javaClass) }?.identifyUnsafe(element)

        fun fold(root: PsiElement): Array<FoldingDescriptor> {
            val descriptors = mutableListOf<FoldingDescriptor>()
            for (identifier in identifiers) {
                val elements = PsiTreeUtil.findChildrenOfType(root, identifier.elementClass())
                for (element in elements) {
                    val translation = identifier.identifyUnsafe(element)
                    if (translation?.foldingElement != null) {
                        val range =
                            if (translation.foldingElement is PsiExpressionList) {
                                translation.foldingElement.textRange.grown(-2).shiftRight(1)
                            } else {
                                translation.foldingElement.textRange
                            }
                        descriptors.add(
                            object : FoldingDescriptor(
                                translation.foldingElement.node,
                                range,
                                FoldingGroup.newGroup("mc.i18n." + translation.key)
                            ) {
                                override fun getPlaceholderText(): String? {
                                    if (translation.formattingError == FormattingError.MISSING) {
                                        return "\"Insufficient parameters for formatting '${translation.text}'\""
                                    }
                                    return "\"${translation.text}\""
                                }
                            }
                        )
                    }
                }
            }
            return descriptors.toTypedArray()
        }

        fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
            for (identifier in identifiers) {
                registrar.registerReferenceProvider(
                    PlatformPatterns.psiElement(identifier.elementClass()),
                    object : PsiReferenceProvider() {
                        override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                        ): Array<PsiReference> {
                            val result = identifier.identifyUnsafe(element)
                            if (result != null) {
                                val referenceElement = result.referenceElement ?: return emptyArray()
                                return arrayOf(
                                    I18nReference(
                                        referenceElement,
                                        TextRange(1, referenceElement.textLength - 1),
                                        false,
                                        result.key,
                                        result.varKey
                                    )
                                )
                            }
                            return emptyArray()
                        }
                    }
                )
            }
        }
    }
}
