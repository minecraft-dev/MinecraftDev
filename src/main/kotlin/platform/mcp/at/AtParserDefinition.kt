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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.mcp.at.gen.parser.AtParser
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class AtParserDefinition : ParserDefinition {

    override fun createLexer(project: Project) = AtLexerAdapter()
    override fun getCommentTokens() = COMMENTS
    override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY
    override fun createParser(project: Project) = AtParser()
    override fun getFileNodeType() = FILE
    override fun createFile(viewProvider: FileViewProvider) = AtFile(viewProvider)
    override fun createElement(node: ASTNode): PsiElement = AtTypes.Factory.createElement(node)

    override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode) =
        map[left.elementType to right.elementType] ?: ParserDefinition.SpaceRequirements.MUST_NOT

    companion object {
        private val COMMENTS = TokenSet.create(AtTypes.COMMENT)

        private val FILE = IFileElementType(Language.findInstance(AtLanguage::class.java))

        private val map: Map<Pair<IElementType, IElementType>, ParserDefinition.SpaceRequirements> = mapOf(
            (AtTypes.KEYWORD_ELEMENT to AtTypes.CLASS_NAME_ELEMENT) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.CLASS_NAME_ELEMENT to AtTypes.FIELD_NAME) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.CLASS_NAME_ELEMENT to AtTypes.FUNCTION) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.CLASS_NAME_ELEMENT to AtTypes.ASTERISK_ELEMENT) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.CLASS_NAME_ELEMENT to AtTypes.COMMENT) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.FIELD_NAME to AtTypes.COMMENT) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.ASTERISK_ELEMENT to AtTypes.COMMENT) to ParserDefinition.SpaceRequirements.MUST,
            (AtTypes.COMMENT to AtTypes.KEYWORD_ELEMENT) to ParserDefinition.SpaceRequirements.MUST_LINE_BREAK,
            (AtTypes.COMMENT to AtTypes.COMMENT) to ParserDefinition.SpaceRequirements.MUST_LINE_BREAK,
            (AtTypes.FUNCTION to AtTypes.COMMENT) to ParserDefinition.SpaceRequirements.MUST,
        )
    }
}
