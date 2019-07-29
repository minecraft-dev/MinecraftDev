/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.i18n.lang.gen.parser.I18nParser
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageUtil
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class I18nParserDefinition : ParserDefinition {
    override fun createParser(project: Project) = I18nParser()
    override fun createLexer(project: Project) = I18nLexerAdapter()
    override fun createFile(viewProvider: FileViewProvider) = I18nFile(viewProvider)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) =
        LanguageUtil.canStickTokensTogetherByLexer(left, right, I18nLexerAdapter())!!

    override fun getStringLiteralElements() = STRING_LITERALS
    override fun getWhitespaceTokens() = WHITE_SPACES
    override fun getFileNodeType() = FILE
    override fun createElement(node: ASTNode) = I18nTypes.Factory.createElement(node)!!
    override fun getCommentTokens() = TokenSet.EMPTY!!

    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val STRING_LITERALS = TokenSet.create(I18nTypes.KEY, I18nTypes.VALUE)

        val FILE = IFileElementType(I18nLanguage)
    }
}
