/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.intellij.openapi.module.Module
import com.intellij.util.lang.JavaVersion
import java.nio.file.Path

class VelocityProjectConfig : ProjectConfig(), MavenCreator, GradleCreator {

    lateinit var mainClass: String

    var velocityApiVersion = ""
    val apiVersion: SemanticVersion
        get() =
            if (velocityApiVersion.isBlank()) SemanticVersion.release() else SemanticVersion.parse(velocityApiVersion)

    override var type: PlatformType = PlatformType.VELOCITY

    val dependencies = mutableListOf<String>()
    fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override val javaVersion: JavaVersion
        get() = when {
            apiVersion >= SemanticVersion.release(3) -> JavaVersion.compose(11)
            else -> JavaVersion.compose(8)
        }

    override fun buildMavenCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: MavenBuildSystem
    ): ProjectCreator {
        return VelocityMavenCreator(rootDirectory, module, buildSystem, this)
    }

    override val compatibleGradleVersions: VersionRange? = null

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return VelocityGradleCreator(rootDirectory, module, buildSystem, this)
    }
}
