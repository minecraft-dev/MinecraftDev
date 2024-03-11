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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MELitExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatementItem
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil.notInTypePosition
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil.validType
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
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.and
import com.intellij.patterns.StandardPatterns.not
import com.intellij.psi.tree.TokenSet
import com.intellij.util.ProcessingContext

class MEExpressionCompletionContributor : CompletionContributor() {
    companion object {
        private val NORMAL_ELEMENT = psiElement()
            .inside(MEStatement::class.java)
            .andNot(psiElement().inside(MELitExpression::class.java))
            .notInTypePosition()
        private val TYPE_PATTERN = psiElement()
            .inside(MEStatement::class.java)
            .validType()
        private val AFTER_END_EXPRESSION_PATTERN = psiElement().afterLeaf(
            psiElement().withElementType(
                TokenSet.create(
                    MEExpressionTypes.TOKEN_IDENTIFIER,
                    MEExpressionTypes.TOKEN_WILDCARD,
                    MEExpressionTypes.TOKEN_RIGHT_PAREN,
                    MEExpressionTypes.TOKEN_RIGHT_BRACKET,
                    MEExpressionTypes.TOKEN_RIGHT_BRACE,
                    MEExpressionTypes.TOKEN_BOOL_LIT,
                    MEExpressionTypes.TOKEN_CLASS,
                    MEExpressionTypes.TOKEN_INT_LIT,
                    MEExpressionTypes.TOKEN_DEC_LIT,
                    MEExpressionTypes.TOKEN_NULL_LIT,
                    MEExpressionTypes.TOKEN_STRING_TERMINATOR,
                )
            )
        )

        private val STATEMENT_KEYWORD_PLACE = psiElement().afterLeaf(
            psiElement().withText("{").withParent(MEStatementItem::class.java)
        )
        private val VALUE_KEYWORD_PLACE = and(
            NORMAL_ELEMENT,
            not(AFTER_END_EXPRESSION_PATTERN),
            not(psiElement().afterLeaf(".")),
        )
        private val CLASS_PLACE = and(
            NORMAL_ELEMENT,
            psiElement()
                .afterLeaf(psiElement().withText(".").withParent(psiElement().withFirstChild(TYPE_PATTERN))),
        )
        private val INSTANCEOF_PLACE = and(
            NORMAL_ELEMENT,
            AFTER_END_EXPRESSION_PATTERN,
        )
    }

    init {
        extend(
            CompletionType.BASIC,
            STATEMENT_KEYWORD_PLACE,
            KeywordCompletionProvider(
                Keyword("return", TailType.INSERT_SPACE),
                Keyword("throw", TailType.INSERT_SPACE),
            )
        )
        extend(
            CompletionType.BASIC,
            VALUE_KEYWORD_PLACE,
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
            CLASS_PLACE,
            KeywordCompletionProvider(
                Keyword("class")
            )
        )
        extend(
            CompletionType.BASIC,
            INSTANCEOF_PLACE,
            KeywordCompletionProvider(
                Keyword("instanceof", TailType.INSERT_SPACE)
            )
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
