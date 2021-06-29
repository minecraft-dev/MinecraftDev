/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.module.Module
import java.nio.file.Path

class SpongeProjectConfig : ProjectConfig(), MavenCreator, GradleCreator {

    lateinit var mainClass: String

    var spongeApiVersion = ""

    override var type = PlatformType.SPONGE

    init {
        type = PlatformType.SPONGE
    }

    val dependencies = mutableListOf<String>()
    fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override val preferredBuildSystem = BuildSystemType.GRADLE

    override fun buildMavenCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: MavenBuildSystem
    ): ProjectCreator {
        val apiVersion = SemanticVersion.parse(spongeApiVersion)
        return if (apiVersion < SpongeConstants.API8) {
            SpongeMavenCreator(rootDirectory, module, buildSystem, this)
        } else {
            Sponge8MavenCreator(rootDirectory, module, buildSystem, this)
        }
    }

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        val apiVersion = SemanticVersion.parse(spongeApiVersion)
        return if (apiVersion < SpongeConstants.API8) {
            SpongeGradleCreator(rootDirectory, module, buildSystem, this)
        } else {
            Sponge8GradleCreator(rootDirectory, module, buildSystem, this)
        }
    }

    override fun configureRootGradle(rootDirectory: Path, buildSystem: GradleBuildSystem) {
        val apiVersion = SemanticVersion.parse(spongeApiVersion)
        if (apiVersion >= SpongeConstants.API8) {
            buildSystem.gradleVersion = SemanticVersion.release(7, 0, 2)
        }
    }
}
