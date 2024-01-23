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

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext

class AtArgsCompletionContributor : CompletionContributor() {
    init {
        for (tokenType in arrayOf(JavaTokenType.STRING_LITERAL, JavaTokenType.TEXT_BLOCK_LITERAL)) {
            extend(
                CompletionType.BASIC,
                PsiJavaPatterns.psiElement(tokenType).withParent(
                    PsiJavaPatterns.psiLiteral(StandardPatterns.string())
                        .insideAnnotationAttribute(MixinConstants.Annotations.AT, "args")
                ),
                Provider,
            )
        }
    }

    object Provider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val literal = parameters.position.parentOfType<PsiLiteral>(withSelf = true) ?: return
            val atAnnotation = literal.parentOfType<PsiAnnotation>() ?: return
            val atCode = atAnnotation.findDeclaredAttributeValue("value")?.constantStringValue ?: return
            val injectionPoint = InjectionPoint.byAtCode(atCode) ?: return
            val escaper = (literal as? PsiLanguageInjectionHost)?.createLiteralTextEscaper() ?: return
            val beforeCursor = buildString {
                escaper.decode(TextRange(1, parameters.offset - (literal as PsiLiteral).textRange.startOffset), this)
            }
            val equalsIndex = beforeCursor.indexOf('=')
            if (equalsIndex == -1) {
                val argsKeys = injectionPoint.getArgsKeys(atAnnotation)
                result.addAllElements(
                    argsKeys.map { completion ->
                        LookupElementBuilder.create(if (completion.contains('=')) completion else "$completion=")
                            .withPresentableText(completion)
                    }
                )
                if (argsKeys.isNotEmpty()) {
                    result.stopHere()
                }
            } else {
                val key = beforeCursor.substring(0, equalsIndex)
                val argsValues = injectionPoint.getArgsValues(atAnnotation, key)
                var prefix = beforeCursor.substring(equalsIndex + 1)
                if (injectionPoint.isArgValueList(atAnnotation, key)) {
                    prefix = prefix.substringAfterLast(',').trimStart()
                }
                result.withPrefixMatcher(prefix).addAllElements(
                    argsValues.map { completion ->
                        when (completion) {
                            is LookupElement -> completion
                            is PsiNamedElement -> LookupElementBuilder.create(completion)
                            else -> LookupElementBuilder.create(completion)
                        }
                    }
                )
                if (argsValues.isNotEmpty()) {
                    result.stopHere()
                }
            }
        }
    }
}
