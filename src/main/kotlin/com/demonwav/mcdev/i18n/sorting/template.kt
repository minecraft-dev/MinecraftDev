/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.sorting

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.project.stateStore
import java.io.File

sealed class TemplateElement

data class Comment(val text: String) : TemplateElement()

data class Key(val matcher: Regex) : TemplateElement()

object EmptyLine : TemplateElement()

data class Template(val elements: List<TemplateElement>) {
    companion object {
        fun parse(s: String): Template =
            Template(
                if (s.isNotEmpty()) {
                    s.split('\n').map {
                        when {
                            it.isEmpty() -> EmptyLine
                            it.startsWith('#') -> Comment(it.substring(1).trim())
                            else -> Key(parseKey(it.trim()))
                        }
                    }
                } else {
                    listOf()
                }
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

    private fun projectFile(project: Project): File =
        File(FileUtil.toSystemDependentName(project.stateStore.getDirectoryStorePath(false)!!), FILE_NAME)

    fun getGlobalTemplateText() = if (globalFile().exists()) globalFile().readText() else ""

    fun getProjectTemplateText(project: Project) =
        projectFile(project).let { if (it.exists()) it.readText() else getGlobalTemplateText() }

    fun getGlobalTemplate() = Template.parse(getGlobalTemplateText())

    fun getProjectTemplate(project: Project) = Template.parse(getProjectTemplateText(project))

    fun writeGlobalTemplate(text: String) = globalFile().writeText(text)

    fun writeProjectTemplate(project: Project, text: String) = projectFile(project).writeText(text)
}
