/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.platform.minecraft.unpick.gen.parser.UnpickParser
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.intellij.lang.ASTNode
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

class UnpickParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = UnpickLexerAdapter()
    override fun createParser(project: Project): PsiParser = UnpickParser()
    override fun getFileNodeType(): IFileElementType = TokenSets.FILE
    override fun getWhitespaceTokens(): TokenSet = TokenSets.WHITE_SPACES
    override fun getCommentTokens(): TokenSet = TokenSets.COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSets.STRINGS
    override fun createElement(node: ASTNode): PsiElement = UnpickTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = UnpickFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements {
        return LanguageUtil.canStickTokensTogetherByLexer(left, right, UnpickLexerAdapter())
    }

    object TokenSets {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val COMMENTS = TokenSet.create(UnpickTypes.COMMENT, UnpickTypes.DOC_COMMENT)
        val STRINGS = TokenSet.create(UnpickTypes.STRING, UnpickTypes.CHAR)
        val IDENTIFIERS = TokenSet.create(UnpickTypes.IDENTIFIER)
        val NUMBERS = TokenSet.create(
            UnpickTypes.INTEGER,
            UnpickTypes.LONG,
            UnpickTypes.FLOAT,
            UnpickTypes.DOUBLE,
            UnpickTypes.MALFORMED_NUMBER,
        )
        val ITEM_KEYWORDS = TokenSet.create(
            UnpickTypes.UNPICK_KW,
            UnpickTypes.GROUP_KW,
            UnpickTypes.TARGET_METHOD_KW,
            UnpickTypes.TARGET_FIELD_KW,
            UnpickTypes.TARGET_ANNOTATION_KW,
        )
        val OPERATORS = TokenSet.create(
            UnpickTypes.SHL,
            UnpickTypes.USHR,
            UnpickTypes.SHR,
            UnpickTypes.PIPE,
            UnpickTypes.CARET,
            UnpickTypes.AMPERSAND,
            UnpickTypes.PLUS,
            UnpickTypes.MINUS,
            UnpickTypes.STAR,
            UnpickTypes.SLASH,
            UnpickTypes.PERCENT,
            UnpickTypes.TILDE,
            UnpickTypes.COLON,
        )

        val FILE = IFileElementType(UnpickLanguage)
    }
}
