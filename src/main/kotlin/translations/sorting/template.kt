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

package com.demonwav.mcdev.translations.sorting

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import java.io.File

sealed class TemplateElement

data class Comment(val text: String) : TemplateElement()

data class Key(val matcher: Regex) : TemplateElement()

object EmptyLine : TemplateElement()

data class Template(val elements: List<TemplateElement>) {
    companion object {
        fun parse(s: String?): Template =
            Template(
                if (!s.isNullOrEmpty()) {
                    s.split('\n').map {
                        when {
                            it.isEmpty() -> EmptyLine
                            it.startsWith('#') -> Comment(it.substring(1).trim())
                            else -> Key(parseKey(it.trim()))
                        }
                    }
                } else {
                    listOf()
                },
            )

        private val keyRegex = Regex("([?!]?[+*]?)([^+*!?]*)([?!]?[+*]?)")

        private fun parseKey(s: String) =
            keyRegex.findAll(s).map {
                parseQuantifier(it.groupValues[1]) +
                    Regex.escape(it.groupValues[2]) +
                    parseQuantifier(it.groupValues[3])
            }.joinToString("", "^", "$").toRegex()

        private fun parseQuantifier(q: String?) =
            when (q) {
                "!" -> "([^.])"
                "!+" -> "([^.]+)"
                "!*" -> "([^.]*)"

                "?" -> "(.)"
                "?+" -> "(..+)"

                "+", "?*" -> "(.+)"
                "*" -> "(.*?)"

                else -> ""
            }
    }
}

object TemplateManager {
    private const val FILE_NAME = "minecraft_localization_template.lang"

    private fun globalFile(): File = File(PathManager.getConfigPath(), FILE_NAME)

    private fun projectFile(project: Project): File? =
        project.stateStore.directoryStorePath?.resolve(FILE_NAME)?.toFile()

    fun getGlobalTemplateText() = if (globalFile().exists()) globalFile().readText() else ""

    fun getProjectTemplateText(project: Project): String? =
        projectFile(project)?.let { if (it.exists()) it.readText() else getGlobalTemplateText() }

    fun getProjectTemplate(project: Project) = Template.parse(getProjectTemplateText(project))

    fun writeGlobalTemplate(text: String) = globalFile().writeText(text)

    fun writeProjectTemplate(project: Project, text: String) = projectFile(project)?.writeText(text)
}
