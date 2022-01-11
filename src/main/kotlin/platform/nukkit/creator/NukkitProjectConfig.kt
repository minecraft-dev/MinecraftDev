/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.nukkit.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.creator.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.MavenCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.nukkit.NukkitLikeConfiguration
import com.demonwav.mcdev.platform.nukkit.data.LoadOrder
import com.demonwav.mcdev.util.VersionRange
import com.intellij.openapi.module.Module
import com.intellij.util.lang.JavaVersion
import java.nio.file.Path

class NukkitProjectConfig() :
    ProjectConfig(), NukkitLikeConfiguration, MavenCreator, GradleCreator {

    override lateinit var mainClass: String

    var loadOrder: LoadOrder = LoadOrder.POSTWORLD
    var minecraftVersion: String = ""

    override var type: PlatformType = PlatformType.NUKKIT
    var prefix: String? = null
    fun hasPrefix() = prefix?.isNotBlank() == true

    var loadBefore: MutableList<String> = mutableListOf()
    fun hasLoadBefore() = listContainsAtLeastOne(loadBefore)
    fun setLoadBefore(string: String) {
        loadBefore.clear()
        loadBefore.addAll(commaSplit(string))
    }

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

    override val javaVersion = JavaVersion.compose(8)

    override val compatibleGradleVersions: VersionRange? = null

    override fun buildMavenCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: MavenBuildSystem
    ): ProjectCreator {
        return NukkitMavenCreator(rootDirectory, module, buildSystem, this)
    }

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return NukkitGradleCreator(rootDirectory, module, buildSystem, this)
    }
}
