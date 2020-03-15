/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

data class BuildDependency(
    var groupId: String = "",
    var artifactId: String = "",
    var version: String = "",
    var mavenScope: String? = null,
    var gradleConfiguration: String? = null
)
