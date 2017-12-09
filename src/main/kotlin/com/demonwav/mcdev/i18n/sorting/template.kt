/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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
                    s.split("\n").map {
                        when {
                            it.isEmpty() -> EmptyLine
                            it.startsWith("#") -> Comment(it.substring(1).trim())
                            else -> Key(it.trim().split('*').joinToString("(.*?)", "^", "$") { Regex.escape(it) }.toRegex())
                        }
                    }
                } else {
                    listOf()
                }
            )
    }
}

object TemplateManager {
    const val FILE_NAME = "minecraft_localization_template.lang"

    fun globalFile(): File = File(PathManager.getConfigPath(), FILE_NAME)

    fun projectFile(project: Project): File =
        File(FileUtil.toSystemDependentName(project.stateStore.getDirectoryStorePath(false)!!), FILE_NAME)

    fun getGlobalTemplateText() = if (globalFile().exists()) globalFile().readText() else ""

    fun getProjectTemplateText(project: Project) = projectFile(project).let { if (it.exists()) it.readText() else getGlobalTemplateText() }

    fun getGlobalTemplate() = Template.parse(getGlobalTemplateText())

    fun getProjectTemplate(project: Project) = Template.parse(getProjectTemplateText(project))

    fun writeGlobalTemplate(text: String) = globalFile().writeText(text)

    fun writeProjectTemplate(project: Project, text: String) = projectFile(project).writeText(text)
}
