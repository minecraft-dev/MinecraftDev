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
import com.demonwav.mcdev.i18n.reference.I18nReference
import com.demonwav.mcdev.i18n.translations.Translation
import com.demonwav.mcdev.i18n.translations.Translation.Companion.FormattingError
import com.demonwav.mcdev.util.evaluate
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
            if (container is PsiPolyadicExpression) {
                return identify(project, container, container.parent, referenceElement)
            }
            if (container is PsiExpressionList && container.parent is PsiCallExpression) {
                val call = container.parent as PsiCallExpression
                val index = container.expressions.indexOf(element)
                val value = element.evaluate("", I18nReference.VARIABLE_MARKER) ?: ""

                for (function in Translation.translationFunctions) {
                    if (function.matches(call, index)) {
                        val result = function.getTranslationKey(call) ?: continue
                        val translationKey = result.trim()
                        val varKey = if (translationKey == value) I18nReference.VARIABLE_MARKER else translationKey
                        val fullKey = translationKey.replace(I18nReference.VARIABLE_MARKER, value)
                        val entries = FileBasedIndex.getInstance().getValues(TranslationIndex.NAME, I18nConstants.DEFAULT_LOCALE, GlobalSearchScope.allScope(project)).merge("")
                        val translation = entries[fullKey]?.text
                        if (translation == null && function.setter) {
                            return null
                        }
                        if (translation != null) {
                            try {
                                val (formatted, superfluousParams) = function.format(translation, call) ?: (translation to -1)
                                return Translation(
                                    if (function.foldParameters) container else call,
                                    referenceElement,
                                    fullKey,
                                    varKey,
                                    formatted,
                                    if (superfluousParams >= 0) FormattingError.SUPERFLUOUS else null,
                                    superfluousParams,
                                    containsVariable = fullKey.contains(I18nReference.VARIABLE_MARKER)
                                )
                            } catch (ignored: MissingFormatArgumentException) {
                                return Translation(
                                    if (function.foldParameters) container else call,
                                    referenceElement,
                                    fullKey,
                                    varKey,
                                    translation,
                                    FormattingError.MISSING,
                                    containsVariable = fullKey.contains(I18nReference.VARIABLE_MARKER)
                                )
                            }
                        } else {
                            return Translation(
                                null,
                                referenceElement,
                                fullKey,
                                varKey,
                                null,
                                containsVariable = fullKey.contains(I18nReference.VARIABLE_MARKER)
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
