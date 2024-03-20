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

import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.BasicExpressionCompletionContributor
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.util.ProcessingContext

class MEExpressionCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            MEExpressionCompletionUtil.STATEMENT_KEYWORD_PLACE,
            KeywordCompletionProvider(
                Keyword("return", TailType.INSERT_SPACE),
                Keyword("throw", TailType.INSERT_SPACE),
            )
        )
        extend(
            CompletionType.BASIC,
            MEExpressionCompletionUtil.VALUE_KEYWORD_PLACE,
            KeywordCompletionProvider(
                Keyword("this"),
                Keyword("super"),
                Keyword("true"),
                Keyword("false"),
                Keyword("null"),
                Keyword("new", TailType.INSERT_SPACE),
            )
        )
        extend(
            CompletionType.BASIC,
            MEExpressionCompletionUtil.CLASS_PLACE,
            KeywordCompletionProvider(
                Keyword("class")
            )
        )
        extend(
            CompletionType.BASIC,
            MEExpressionCompletionUtil.INSTANCEOF_PLACE,
            KeywordCompletionProvider(
                Keyword("instanceof", TailType.INSERT_SPACE)
            )
        )
        extend(
            CompletionType.BASIC,
            MEExpressionCompletionUtil.FROM_BYTECODE_PLACE,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.originalFile.project
                    result.addAllElements(
                        MEExpressionCompletionUtil.getCompletionVariantsFromBytecode(project, parameters.position)
                    )
                }
            }
        )
    }

    private class KeywordCompletionProvider(
        private vararg val keywords: Keyword,
    ) : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            result.addAllElements(
                keywords.map { keyword ->
                    var lookupItem =
                        BasicExpressionCompletionContributor.createKeywordLookupItem(parameters.position, keyword.name)
                    if (keyword.tailType != TailType.NONE) {
                        lookupItem = object : TailTypeDecorator<LookupElement>(lookupItem) {
                            override fun computeTailType(context: InsertionContext?) = keyword.tailType
                        }
                    }
                    lookupItem
                }
            )
        }
    }

    private class Keyword(val name: String, val tailType: TailType = TailType.NONE)
}
