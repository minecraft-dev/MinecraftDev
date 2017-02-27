/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag

@Tag("buildDependency")
data class BuildDependency(
    @Attribute var artifactId: String? = null,
    @Attribute var groupId: String? = null,
    @Attribute var version: String? = null,
    @Attribute var scope: String? = null
)
