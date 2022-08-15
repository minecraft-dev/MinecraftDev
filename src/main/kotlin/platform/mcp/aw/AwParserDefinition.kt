/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
