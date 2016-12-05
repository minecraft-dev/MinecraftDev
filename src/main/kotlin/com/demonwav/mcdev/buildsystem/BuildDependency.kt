/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

data class BuildDependency(
    var artifactId: String? = null,
    var groupId: String? = null,
    var version: String? = null,
    var scope: String? = null
)
