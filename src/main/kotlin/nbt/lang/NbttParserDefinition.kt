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

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.parser.NbttParser
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageUtil
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class NbttParserDefinition : ParserDefinition {

    override fun createParser(project: Project) = NbttParser()
    override fun createLexer(project: Project) = NbttLexerAdapter()
    override fun createFile(viewProvider: FileViewProvider) = NbttFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) =
        LanguageUtil.canStickTokensTogetherByLexer(left, right, NbttLexerAdapter())

    override fun getStringLiteralElements() = STRING_LITERALS
    override fun getWhitespaceTokens() = WHITE_SPACES
    override fun getFileNodeType() = FILE
    override fun createElement(node: ASTNode) = NbttTypes.Factory.createElement(node)!!
    override fun getCommentTokens() = TokenSet.EMPTY!!

    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val STRING_LITERALS = TokenSet.create(NbttTypes.STRING_LITERAL)

        val FILE = IFileElementType(NbttLanguage)

        val NBTT_CONTAINERS = TokenSet.create(
            NbttTypes.BYTE_ARRAY,
            NbttTypes.INT_ARRAY,
            NbttTypes.LONG_ARRAY,
            NbttTypes.COMPOUND,
            NbttTypes.LIST,
        )

        val NBTT_OPEN_BRACES = TokenSet.create(
            NbttTypes.LPAREN,
            NbttTypes.LBRACE,
            NbttTypes.LBRACKET,
        )

        val NBTT_CLOSE_BRACES = TokenSet.create(
            NbttTypes.RPAREN,
            NbttTypes.RBRACE,
            NbttTypes.RBRACKET,
        )

        val NBTT_BRACES = TokenSet.orSet(NBTT_OPEN_BRACES, NBTT_CLOSE_BRACES)
    }
}
