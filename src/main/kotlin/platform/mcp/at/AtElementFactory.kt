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

@file:Suppress("unused") // this file represents an API

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtArgument
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtAsterisk
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFuncName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtKeyword
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtReturnValue
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFileFactory

object AtElementFactory {

    fun createFile(project: Project, text: String): AtFile {
        return PsiFileFactory.getInstance(project).createFileFromText("name", AtFileType, text) as AtFile
    }

    fun createArgument(project: Project, text: String): AtArgument {
        val line = "public c.c f($text)V"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.FUNCTION)!!
            .findChildByType(AtTypes.ARGUMENT)!!.psi as AtArgument
    }

    fun createClassName(project: Project, name: String): AtClassName {
        val line = "public $name f(Z)V"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.CLASS_NAME)!!.psi as AtClassName
    }

    fun createEntry(project: Project, entry: String): AtEntry {
        val file = createFile(project, entry)
        return file.firstChild as AtEntry
    }

    fun createFieldName(project: Project, name: String): AtFieldName {
        val line = "public c.c $name"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.FIELD_NAME)!!.psi as AtFieldName
    }

    fun createFuncName(project: Project, name: String): AtFuncName {
        val line = "public c.c $name(Z)V"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.FUNCTION)!!
            .findChildByType(AtTypes.FUNC_NAME)!!.psi as AtFuncName
    }

    fun createFunction(project: Project, function: String): AtFunction {
        val line = "public c.c $function"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.FUNCTION)!!.psi as AtFunction
    }

    fun createAsterisk(project: Project): AtAsterisk {
        val line = "public c.c *"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.FUNCTION)!!
            .findChildByType(AtTypes.ASTERISK)!!.psi as AtAsterisk
    }

    fun createKeyword(project: Project, keyword: Keyword): AtKeyword {
        val line = "${keyword.text} c.c f(Z)V"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.KEYWORD)!!.psi as AtKeyword
    }

    fun createReturnValue(project: Project, returnValue: String): AtReturnValue {
        val line = "public c.c f(Z)$returnValue"
        val file = createFile(project, line)

        return file.firstChild.node.findChildByType(AtTypes.FUNCTION)!!
            .findChildByType(AtTypes.RETURN_VALUE)!!.psi as AtReturnValue
    }

    fun createComment(project: Project, comment: String): PsiComment {
        val line = "# $comment"
        val file = createFile(project, line)

        return file.node.findChildByType(AtTypes.COMMENT)!!.psi as PsiComment
    }

    enum class Keyword(val text: String) {
        PRIVATE("private"),
        PRIVATE_MINUS_F("private-f"),
        PRIVATE_PLUS_F("private+f"),
        PROTECTED("protected"),
        PROTECTED_MINUS_F("protected-f"),
        PROTECTED_PLUS_F("protected+f"),
        PUBLIC("public"),
        PUBLIC_MINUS_F("public-f"),
        PUBLIC_PLUS_F("public+f"),
        DEFAULT("default"),
        DEFAULT_MINUS_F("default-f"),
        DEFAULT_PLUS_F("default+f"),
        ;

        companion object {
            fun match(s: String) = values().firstOrNull { it.text == s }
            fun softMatch(s: String) = values().filter { it.text.contains(s) }
        }
    }
}
