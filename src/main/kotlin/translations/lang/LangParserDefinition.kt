/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.translations.lang

import com.demonwav.mcdev.translations.lang.gen.parser.LangParser
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageUtil
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class LangParserDefinition : ParserDefinition {
    override fun createParser(project: Project) = LangParser()
    override fun createLexer(project: Project) = LangLexerAdapter()
    override fun createFile(viewProvider: FileViewProvider) = LangFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) =
        LanguageUtil.canStickTokensTogetherByLexer(left, right, LangLexerAdapter())

    override fun getStringLiteralElements() = STRING_LITERALS
    override fun getWhitespaceTokens() = WHITE_SPACES
    override fun getFileNodeType() = FILE
    override fun createElement(node: ASTNode) = LangTypes.Factory.createElement(node)!!
    override fun getCommentTokens() = TokenSet.EMPTY!!

    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val STRING_LITERALS = TokenSet.create(LangTypes.KEY, LangTypes.VALUE)

        val FILE = IFileElementType(MCLangLanguage)
    }
}
