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
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.isNullOrEmpty
import org.jetbrains.annotations.Contract

abstract class ProjectConfiguration {

    lateinit var pluginName: String
    lateinit var pluginVersion: String
    lateinit var mainClass: String
    lateinit var description: String
    var website: String? = null
    lateinit var type: PlatformType

    val authors = mutableListOf<String>()

    lateinit var module: Module

    var isFirst = false

    abstract fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator)

    fun hasAuthors() = listContainsAtLeastOne(authors)
    fun setAuthors(string: String) {
        authors.clear()
        authors.addAll(commaSplit(string))
    }

    fun hasDescription() = description.isNotBlank()

    protected fun commaSplit(string: String) =
        string.trim().replace("[\\[\\]]".toRegex(), "").split("\\s*,\\s*".toRegex()).toTypedArray()

    @Contract("null -> false")
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
