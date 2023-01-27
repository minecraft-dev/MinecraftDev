/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem

import java.util.EnumSet

enum class BuildSystemType {
    MAVEN,
    GRADLE,
}

data class BuildDependency(
    val groupId: String = "",
    val artifactId: String = "",
    val version: String = "",
    val mavenScope: String? = null,
    val gradleConfiguration: String? = null
)

data class BuildRepository(
    var id: String = "",
    var url: String = "",
    val buildSystems: EnumSet<BuildSystemType> = EnumSet.allOf(BuildSystemType::class.java)
)
