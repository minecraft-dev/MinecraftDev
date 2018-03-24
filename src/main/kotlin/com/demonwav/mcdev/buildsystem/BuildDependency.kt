/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

data class BuildDependency(
    var artifactId: String = "",
    var groupId: String = "",
    var version: String = "",
    var scope: String = ""
)
