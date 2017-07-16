/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.translations.identifiers

import com.demonwav.mcdev.i18n.findDefaultProperties
import com.demonwav.mcdev.i18n.reference.I18nReference
import com.demonwav.mcdev.i18n.translations.Translation
import com.demonwav.mcdev.util.evaluate
import com.demonwav.mcdev.util.referencedMethod
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiPolyadicExpression
import java.util.MissingFormatArgumentException

abstract class TranslationIdentifier<T : PsiElement> {
    @SuppressWarnings("unchecked")
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

                val method = call.referencedMethod
                for (function in Translation.translationFunctions) {
                    if (function.matches(method, index)) {
                        val result = function.getTranslationKey(call) ?: continue
                        val translationKey = result.second.trim()
                        val fullKey = translationKey.replace(I18nReference.VARIABLE_MARKER, value)
                        val properties = project.findDefaultProperties(key = fullKey)
                        val translation = if (properties.isNotEmpty()) properties[0].value else null
                        if (translation == null && function.setter) {
                            return null
                        }
                        if (translation != null) {
                            try {
                                return Translation(if (function.foldParameters) container else call,
                                    if (result.first) referenceElement else null,
                                    fullKey,
                                    translationKey,
                                    function.format(translation, call) ?: translation,
                                    containsVariable = fullKey.contains(I18nReference.VARIABLE_MARKER))
                            } catch (ignored: MissingFormatArgumentException) {
                                return Translation(if (function.foldParameters) container else call,
                                    if (result.first) referenceElement else null,
                                    fullKey,
                                    translationKey,
                                    translation,
                                    true,
                                    containsVariable = fullKey.contains(I18nReference.VARIABLE_MARKER))
                            }
                        } else {
                            return Translation(null,
                                if (result.first) referenceElement else null,
                                fullKey,
                                translationKey,
                                null,
                                containsVariable = fullKey.contains(I18nReference.VARIABLE_MARKER))
                        }
                    }
                }
                return null
            }
            return null
        }
    }
}
