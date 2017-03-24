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
import com.intellij.util.xml.SubTag

interface MavenProjectXml : DomElement {

    @get:SubTag("groupId")
    val groupId: GroupId
    @get:SubTag("artifactId")
    val artifactId: ArtifactId
    @get:SubTag("version")
    val version: Version

    @get:SubTag("name")
    val name: Name
    @get:SubTag("url")
    val url: Url

    @get:SubTag("repositories")
    val repositories: Repositories
    @get:SubTag("dependencies")
    val dependencies: Dependencies
}
