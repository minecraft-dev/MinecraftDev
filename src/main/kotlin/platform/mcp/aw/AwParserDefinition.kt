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

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.platform.mcp.aw.gen.parser.AwParser
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class AwParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = AwLexerAdapter()
    override fun createParser(project: Project): PsiParser = AwParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createElement(node: ASTNode): PsiElement = AwTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = AwFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
        return LanguageUtil.canStickTokensTogetherByLexer(left, right, AwLexerAdapter())
    }

    companion object {
        private val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        private val COMMENTS = TokenSet.create(AwTypes.COMMENT)

        private val FILE = IFileElementType(Language.findInstance(AwLanguage::class.java))
    }
}
