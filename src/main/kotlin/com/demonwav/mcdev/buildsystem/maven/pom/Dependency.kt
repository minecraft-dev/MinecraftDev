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

interface Dependency : DomElement {

    @get:SubTag("groupId")
    val groupId: GroupId
    @get:SubTag("artifactId")
    val artifactId: ArtifactId
    @get:SubTag("version")
    val version: Version
    @get:SubTag("scope")
    val scope: Scope
}
