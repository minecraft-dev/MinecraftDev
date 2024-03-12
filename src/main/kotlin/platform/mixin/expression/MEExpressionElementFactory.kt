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

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEClassConstantExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.util.IncorrectOperationException

class MEExpressionElementFactory(private val project: Project) {
    fun createFile(text: String): MEExpressionFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            "dummy.mixinextrasexpression",
            MEExpressionFileType,
            text
        ) as MEExpressionFile
    }

    fun createStatement(text: String): MEStatement {
        return createFile("do {$text}").statements.singleOrNull()
            ?: throw IncorrectOperationException("'$text' is not a statement")
    }

    fun createExpression(text: String): MEExpression {
        return (createStatement(text) as? MEExpressionStatement)?.expression
            ?: throw IncorrectOperationException("'$text' is not an expression")
    }

    fun createName(text: String): MEName {
        return (createExpression(text) as? MENameExpression)?.meName
            ?: throw IncorrectOperationException("'$text' is not a name")
    }

    fun createIdentifier(text: String): PsiElement {
        return createName(text).identifierElement
            ?: throw IncorrectOperationException("'$text' is not an identifier")
    }

    fun createType(text: String): METype {
        return (createExpression("$text.class") as? MEClassConstantExpression)?.type
            ?: throw IncorrectOperationException("'$text' is not a type")
    }

    fun createType(name: MEName) = createType(name.text)
}

val Project.meExpressionElementFactory get() = MEExpressionElementFactory(this)
