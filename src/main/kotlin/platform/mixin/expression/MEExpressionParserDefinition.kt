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

import com.demonwav.mcdev.platform.mixin.expression.gen.MEExpressionParser
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionFile
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionTokenSets
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class MEExpressionParserDefinition : ParserDefinition {

    override fun createLexer(project: Project) = MEExpressionLexerAdapter()
    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY
    override fun getStringLiteralElements() = MEExpressionTokenSets.STRINGS
    override fun createParser(project: Project) = MEExpressionParser()
    override fun getFileNodeType() = FILE
    override fun createFile(viewProvider: FileViewProvider) = MEExpressionFile(viewProvider)
    override fun createElement(node: ASTNode): PsiElement = MEExpressionTypes.Factory.createElement(node)
}

val FILE = IFileElementType(MEExpressionLanguage)
