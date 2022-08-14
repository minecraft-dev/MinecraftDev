/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.lang.JavaVersion

private val bracketRegex = Regex("[\\[\\]]")
private val commaRegex = Regex("\\s*,\\s*")

abstract class ProjectConfig {

    abstract var type: PlatformType
    open val preferredBuildSystem: BuildSystemType? = null

    lateinit var pluginName: String

    var website: String? = null
    fun hasWebsite() = !website.isNullOrBlank()

    val authors: MutableList<String> = mutableListOf()
    fun hasAuthors() = listContainsAtLeastOne(authors)
    fun setAuthors(string: String) {
        authors.clear()
        authors.addAll(commaSplit(string))
    }

    var description: String? = null
    fun hasDescription() = description?.isNotBlank() == true

    abstract val javaVersion: JavaVersion

    protected fun commaSplit(string: String): List<String> {
        return if (!string.isBlank()) {
            string.trim().replace(bracketRegex, "").split(commaRegex).toList()
        } else {
            emptyList()
        }
    }

    protected fun listContainsAtLeastOne(list: MutableList<String>?): Boolean {
        if (list.isNullOrEmpty()) {
            return false
        }

        list?.removeIf(String::isBlank)

        return list?.size != 0
    }
}
