/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.identification

import com.demonwav.mcdev.translations.identification.TranslationInstance.Companion.FormattingError
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.index.merge
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import java.util.MissingFormatArgumentException

abstract class TranslationIdentifier<T : PsiElement> {
    @Suppress("UNCHECKED_CAST")
    fun identifyUnsafe(element: PsiElement): TranslationInstance? {
        return identify(element as T)
    }

    abstract fun identify(element: T): TranslationInstance?

    abstract fun elementClass(): Class<T>

    companion object {
        val INSTANCES = listOf(LiteralTranslationIdentifier(), ReferenceTranslationIdentifier())

        fun identify(
            project: Project,
            element: PsiExpression,
            container: PsiElement,
            referenceElement: PsiElement
        ): TranslationInstance? {
            if (container is PsiExpressionList && container.parent is PsiCallExpression) {
                val call = container.parent as PsiCallExpression
                val index = container.expressions.indexOf(element)

                for (function in TranslationInstance.translationFunctions) {
                    if (function.matches(call, index)) {
                        val translationKey = function.getTranslationKey(call, referenceElement) ?: continue
                        val entries = TranslationIndex.getAllDefaultEntries(project).merge("")
                        val translation = entries[translationKey.full]?.text
                        if (translation != null) {
                            val foldingElement = when (function.foldParameters) {
                                TranslationFunction.FoldingScope.CALL -> call
                                TranslationFunction.FoldingScope.PARAMETER -> element
                                TranslationFunction.FoldingScope.PARAMETERS -> container
                            }
                            try {
                                val (formatted, superfluousParams) = function.format(translation, call)
                                    ?: (translation to -1)
                                return TranslationInstance(
                                    foldingElement,
                                    function.matchedIndex,
                                    referenceElement,
                                    translationKey,
                                    formatted,
                                    if (superfluousParams >= 0) FormattingError.SUPERFLUOUS else null,
                                    superfluousParams
                                )
                            } catch (ignored: MissingFormatArgumentException) {
                                return TranslationInstance(
                                    foldingElement,
                                    function.matchedIndex,
                                    referenceElement,
                                    translationKey,
                                    translation,
                                    FormattingError.MISSING
                                )
                            }
                        } else {
                            return TranslationInstance(
                                null,
                                function.matchedIndex,
                                referenceElement,
                                translationKey,
                                null
                            )
                        }
                    }
                }
                return null
            }
            return null
        }
    }
}
