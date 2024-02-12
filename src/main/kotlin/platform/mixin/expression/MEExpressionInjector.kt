/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.findContainingModifierList
import com.demonwav.mcdev.util.findContainingNameValuePair
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.psi.ElementManipulators
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.psi.impl.source.tree.injected.JavaConcatenationToInjectorAdapter
import com.intellij.psi.util.PsiLiteralUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.SmartList

class MEExpressionInjector : MultiHostInjector {
    companion object {
        private val ELEMENTS = listOf(PsiLiteralExpression::class.java)
        private val PRIMARY_ELEMENT_KEY = Key.create<PrimaryElement>("mcdev.anchorModCount")
    }

    private data class PrimaryElement(val modCount: Long, val element: PsiElement)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val project = context.project
        val (anchor, operands) = JavaConcatenationToInjectorAdapter(project).computeAnchorAndOperands(context)

        val nameValuePair = anchor.findContainingNameValuePair() ?: return
        if (nameValuePair.name != "value" && nameValuePair.name != null) {
            return
        }
        val expressionAnnotation = nameValuePair.parentOfType<PsiAnnotation>() ?: return
        if (!expressionAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION)) {
            return
        }

        val modCount = PsiModificationTracker.getInstance(project).modificationCount
        val primaryElement = PrimaryElement(modCount, context)
        val existingElement = (anchor as UserDataHolderEx).putUserDataIfAbsent(PRIMARY_ELEMENT_KEY, primaryElement)
        if (existingElement !== primaryElement && existingElement.modCount == modCount && context != existingElement.element) {
            return
        }

        val modifierList = expressionAnnotation.findContainingModifierList() ?: return

        var isFrankenstein = false
        registrar.startInjecting(MEExpressionLanguage)

        for (annotation in modifierList.annotations) {
            if (annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)) {
                val idExpr = annotation.findDeclaredAttributeValue("id") ?: continue
                var needsPrefix = true
                iterateConcatenation(idExpr) { op ->
                    if (op is PsiLanguageInjectionHost) {
                        for (textRange in getTextRanges(op)) {
                            val prefix = " class ".takeIf { needsPrefix }
                            needsPrefix = false
                            registrar.addPlace(prefix, null, op, textRange)
                        }
                    } else {
                        isFrankenstein = true
                    }
                }
            } else if (annotation == expressionAnnotation) {
                val places = mutableListOf<Pair<PsiLanguageInjectionHost, TextRange>>()
                for (operand in operands) {
                    iterateConcatenation(operand) { op ->
                        if (op is PsiLanguageInjectionHost) {
                            for (textRange in getTextRanges(op)) {
                                places += op to textRange
                            }
                        } else {
                            isFrankenstein = true
                        }
                    }
                }
                if (places.isNotEmpty()) {
                    for ((i, place) in places.withIndex()) {
                        val (host, range) = place
                        val prefix = " do { ".takeIf { i == 0 }
                        val suffix = " }".takeIf { i == places.size - 1 }
                        registrar.addPlace(prefix, suffix, host, range)
                    }
                }
            }
        }

        registrar.doneInjecting()

        if (isFrankenstein) {
            InjectedLanguageUtil.putInjectedFileUserData(
                context,
                MEExpressionLanguage,
                InjectedLanguageManager.FRANKENSTEIN_INJECTION,
                true
            )
        }
    }

    private fun iterateConcatenation(element: PsiElement, consumer: (PsiElement) -> Unit) {
        when (element) {
            is PsiParenthesizedExpression -> {
                val inner = PsiUtil.skipParenthesizedExprDown(element) ?: return
                iterateConcatenation(inner, consumer)
            }
            is PsiPolyadicExpression -> {
                if (element.operationTokenType == JavaTokenType.PLUS) {
                    for (operand in element.operands) {
                        iterateConcatenation(operand, consumer)
                    }
                } else {
                    consumer(element)
                }
            }
            else -> consumer(element)
        }
    }

    private fun getTextRanges(host: PsiLanguageInjectionHost): List<TextRange> {
        if (host is PsiLiteralExpression && host.isTextBlock) {
            val textRange = ElementManipulators.getValueTextRange(host)
            val indent = PsiLiteralUtil.getTextBlockIndent(host)
            if (indent <= 0) {
                return listOf(textRange)
            }

            val text = (host as PsiElement).text
            var startOffset = textRange.startOffset + indent
            var endOffset = text.indexOf('\n', startOffset)
            val result = SmartList<TextRange>()
            while (endOffset > 0) {
                endOffset++
                result.add(TextRange(startOffset, endOffset))
                startOffset = endOffset + indent
                endOffset = text.indexOf('\n', startOffset)
            }
            endOffset = textRange.endOffset
            if (startOffset < endOffset) {
                result.add(TextRange(startOffset, endOffset))
            }
            return result
        } else {
            return listOf(ElementManipulators.getValueTextRange(host))
        }
    }

    override fun elementsToInjectIn() = ELEMENTS
}
