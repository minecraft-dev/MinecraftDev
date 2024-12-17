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

import com.demonwav.mcdev.platform.mcp.aw.AwElementFactory.Access.entries
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwHeader
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFileFactory

object AwElementFactory {

    fun createFile(project: Project, text: String): AwFile {
        return PsiFileFactory.getInstance(project).createFileFromText("name", AwFileType, text) as AwFile
    }

    fun createEntry(project: Project, entry: String): AwEntry {
        val file = createFile(project, entry)
        return file.firstChild as AwEntry
    }

    fun createComment(project: Project, comment: String): PsiComment {
        val line = "# $comment"
        val file = createFile(project, line)

        return file.node.findChildByType(AwTypes.COMMENT)!!.psi as PsiComment
    }

    fun createHeader(project: Project): AwHeader {
        val file = createFile(project, "accessWidener v2 named\n")
        return file.firstChild as AwHeader
    }

    enum class Access(val text: String) {
        EXTENDABLE("extendable"),
        ACCESSIBLE("accessible"),
        MUTABLE("mutable"),
        TRANSITIVE_EXTENDABLE("transitive-extendable"),
        TRANSITIVE_ACCESSIBLE("transitive-accessible"),
        TRANSITIVE_MUTABLE("transitive-mutable"),
        ;

        companion object {
            fun match(s: String) = entries.firstOrNull { it.text == s }
            fun softMatch(s: String) = entries.filter { it.text.contains(s) }
        }
    }
}
