/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module
import java.nio.file.Path

class VelocityProjectConfig : ProjectConfig(), GradleCreator {

    lateinit var mainClass: String

    var velocityApiVersion = ""

    override var type: PlatformType = PlatformType.VELOCITY

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return VelocityGradleCreator(rootDirectory, module, buildSystem, this)
    }
}
