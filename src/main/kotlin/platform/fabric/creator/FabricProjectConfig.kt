/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.util.License
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.intellij.openapi.module.Module
import com.intellij.util.lang.JavaVersion
import java.nio.file.Path

class FabricProjectConfig : ProjectConfig(), GradleCreator {

    var yarnVersion = ""
    var yarnClassifier: String? = "v2"

    // Minecraft does not follow semver in the snapshots
    var mcVersion = ""
    var semanticMcVersion = SemanticVersion.release()
    var loaderVersion = SemanticVersion.release()
    var apiVersion: SemanticVersion? = null
    var apiMavenLocation: String? = null
    var loomVersion = SemanticVersion.release()
    var gradleVersion = SemanticVersion.release()
    var environment = Side.NONE
    var entryPoints: List<EntryPoint> = arrayListOf()
    var modRepo: String? = null
    var mixins = false
    var genSources = true
    var license: License? = null

    override var type = PlatformType.FABRIC

    override val preferredBuildSystem = BuildSystemType.GRADLE

    override val javaVersion: JavaVersion
        get() = MinecraftVersions.requiredJavaVersion(semanticMcVersion)

    override val compatibleGradleVersions: VersionRange
        get() = VersionRange.fixed(gradleVersion)

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return FabricProjectCreator(
            rootDirectory,
            module,
            buildSystem,
            this
        )
    }

    override fun configureRootGradle(rootDirectory: Path, buildSystem: GradleBuildSystem) {
        buildSystem.gradleVersion =
            if (semanticMcVersion >= MinecraftVersions.MC1_17) SemanticVersion.release(7, 4, 2) else gradleVersion
    }
}
