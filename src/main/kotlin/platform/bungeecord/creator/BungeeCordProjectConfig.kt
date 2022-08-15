/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.bukkit.BukkitLikeConfiguration
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.intellij.openapi.module.Module
import com.intellij.util.lang.JavaVersion
import java.nio.file.Path

class BungeeCordProjectConfig(override var type: PlatformType) :
    ProjectConfig(), MavenCreator, GradleCreator, BukkitLikeConfiguration {

    override lateinit var mainClass: String

    var minecraftVersion = ""
    val semanticMinecraftVersion: SemanticVersion
        get() = if (minecraftVersion.isBlank()) SemanticVersion.release() else SemanticVersion.parse(minecraftVersion)

    override val dependencies = mutableListOf<String>()
    override fun hasDependencies() = listContainsAtLeastOne(dependencies)
    override fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override val softDependencies = mutableListOf<String>()
    override fun hasSoftDependencies() = listContainsAtLeastOne(softDependencies)
    override fun setSoftDependencies(string: String) {
        softDependencies.clear()
        softDependencies.addAll(commaSplit(string))
    }

    override val preferredBuildSystem = BuildSystemType.MAVEN

    override val javaVersion: JavaVersion
        get() = MinecraftVersions.requiredJavaVersion(semanticMinecraftVersion)

    override val compatibleGradleVersions: VersionRange? = null

    override fun buildMavenCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: MavenBuildSystem
    ): ProjectCreator {
        return BungeeCordMavenCreator(rootDirectory, module, buildSystem, this)
    }

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return BungeeCordGradleCreator(rootDirectory, module, buildSystem, this)
    }
}
