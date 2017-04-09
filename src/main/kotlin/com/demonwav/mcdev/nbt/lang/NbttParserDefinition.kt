/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.parser.NbttParser
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
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

    override fun spaceExistanceTypeBetweenTokens(left: ASTNode, right: ASTNode) =
        LanguageUtil.canStickTokensTogetherByLexer(left, right, NbttLexerAdapter())!!

    override fun getStringLiteralElements() = STRING_LITERALS
    override fun getWhitespaceTokens() = WHITE_SPACES
    override fun getFileNodeType() = FILE
    override fun createElement(node: ASTNode) = NbttTypes.Factory.createElement(node)!!
    override fun getCommentTokens() = TokenSet.EMPTY!!

    companion object {
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
        val STRING_LITERALS = TokenSet.create(NbttTypes.STRING_LITERAL)

        val FILE = IFileElementType(Language.findInstance(NbttLanguage::class.java))

        val NBTT_CONTAINERS = TokenSet.create(NbttTypes.BYTE_ARRAY, NbttTypes.INT_ARRAY, NbttTypes.COMPOUND, NbttTypes.LIST)

        val NBTT_OPEN_BRACES = TokenSet.create(
            NbttTypes.LPAREN,
            NbttTypes.LBRACE,
            NbttTypes.LBRACKET
        )
        val NBTT_CLOSE_BRACES = TokenSet.create(
            NbttTypes.RPAREN,
            NbttTypes.RBRACE,
            NbttTypes.RBRACKET
        )
        val NBTT_BRACES = TokenSet.orSet(NBTT_OPEN_BRACES, NBTT_CLOSE_BRACES)
    }
}
