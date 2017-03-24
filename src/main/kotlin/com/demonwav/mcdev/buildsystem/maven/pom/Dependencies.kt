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

interface Dependencies : DomElement {

    @get:SubTagList("dependency")
    val dependencies: List<Dependency>

    fun addDependency(): Dependency
}
