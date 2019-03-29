/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.isNullOrEmpty

private val bracketRegex = Regex("[\\[\\]]")
private val commaRegex = Regex("\\s*,\\s*")

abstract class ProjectConfiguration {

    data class BaseConfigs(
        val pluginName: String,
        val pluginVersion: String,
        val mainClass: String,
        val description: String? = null,
        val website: String? = null,
        val authors: MutableList<String> = mutableListOf()
    )

    var base: BaseConfigs? = null

    abstract var type: PlatformType

    abstract fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator)

    fun hasAuthors() = listContainsAtLeastOne(base?.authors)
    fun setAuthors(string: String) {
        base?.authors?.let { authors ->
            authors.clear()
            authors.addAll(commaSplit(string))
        }
    }

    fun hasDescription() = base?.description?.isNotBlank() == true

    protected fun commaSplit(string: String): List<String> {
        return if (!string.isBlank()) {
            string.trim().replace(bracketRegex, "").split(commaRegex).toList()
        } else {
            emptyList()
        }
    }

    fun listContainsAtLeastOne(list: MutableList<String>?): Boolean {
        if (list.isNullOrEmpty()) {
            return false
        }

        list?.removeIf(String::isBlank)

        return list?.size != 0
    }

    protected fun getMainClassDirectory(files: Array<String>, file: VirtualFile): VirtualFile {
        var movingFile = file
        for (i in 0 until (files.size - 1)) {
            val s = files[i]
            val temp = movingFile.findChild(s)
            movingFile = if (temp != null && temp.isDirectory) {
                temp
            } else {
                movingFile.createChildDirectory(this, s)
            }
        }

        return movingFile
    }
}
