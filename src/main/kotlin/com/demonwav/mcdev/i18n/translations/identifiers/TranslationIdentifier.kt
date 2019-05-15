/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.translations.identifiers

import com.demonwav.mcdev.i18n.I18nConstants
import com.demonwav.mcdev.i18n.index.TranslationIndex
import com.demonwav.mcdev.i18n.index.merge
import com.demonwav.mcdev.i18n.translations.Translation
import com.demonwav.mcdev.i18n.translations.Translation.Companion.FormattingError
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import java.util.MissingFormatArgumentException

abstract class TranslationIdentifier<T : PsiElement> {
    @Suppress("UNCHECKED_CAST")
    fun identifyUnsafe(element: PsiElement): Translation? {
        return identify(element as T)
    }

    abstract fun identify(element: T): Translation?

    abstract fun elementClass(): Class<T>

    companion object {
        fun identify(project: Project, element: PsiExpression, container: PsiElement, referenceElement: PsiElement): Translation? {
            if (container is PsiExpressionList && container.parent is PsiCallExpression) {
                val call = container.parent as PsiCallExpression
                val index = container.expressions.indexOf(element)

                for (function in Translation.translationFunctions) {
                    if (function.matches(call, index)) {
                        val translationKey = function.getTranslationKey(call) ?: continue
                        val entries = FileBasedIndex.getInstance().getValues(
                            TranslationIndex.NAME,
                            I18nConstants.DEFAULT_LOCALE,
                            GlobalSearchScope.allScope(project)
                        ).merge("")
                        val translation = entries[translationKey.full]?.text
                        if (translation != null) {
                            try {
                                val (formatted, superfluousParams) = function.format(translation, call) ?: (translation to -1)
                                return Translation(
                                    if (function.foldParameters) container else call,
                                    referenceElement,
                                    translationKey,
                                    formatted,
                                    if (superfluousParams >= 0) FormattingError.SUPERFLUOUS else null,
                                    superfluousParams
                                )
                            } catch (ignored: MissingFormatArgumentException) {
                                return Translation(
                                    if (function.foldParameters) container else call,
                                    referenceElement,
                                    translationKey,
                                    translation,
                                    FormattingError.MISSING
                                )
                            }
                        } else {
                            return Translation(
                                null,
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
