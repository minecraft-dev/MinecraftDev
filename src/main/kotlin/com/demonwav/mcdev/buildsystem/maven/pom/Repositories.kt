/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven.pom

import com.intellij.util.xml.DomElement
import com.intellij.util.xml.SubTagList

interface Repositories : DomElement {

    @get:SubTagList("repository")
    val repositories: List<Repository>

    fun addRepository(): Repository
}
