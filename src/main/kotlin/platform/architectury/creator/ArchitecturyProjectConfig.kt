/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.License
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.VersionRange
import com.intellij.openapi.module.Module
import com.intellij.util.lang.JavaVersion
import java.nio.file.Path

class ArchitecturyProjectConfig : ProjectConfig(), GradleCreator {

    var mcVersion: SemanticVersion = SemanticVersion.release()
    var forgeVersion: SemanticVersion = SemanticVersion.release()
    var forgeVersionText: String = ""
    var fabricLoaderVersion = SemanticVersion.release()
    var fabricApiVersion: SemanticVersion = SemanticVersion.release()
    var architecturyApiVersion: SemanticVersion = SemanticVersion.release()
    var loomVersion = SemanticVersion.release()
    private var gradleVersion = SemanticVersion.release()
    val architecturyGroup: String
        get() = when {
            architecturyApiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
            else -> "me.shedaniel"
        }
    val architecturyPackage: String
        get() = when {
            architecturyApiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
            else -> "me.shedaniel.architectury"
        }
    var modRepo: String? = null
    fun hasRepo() = !modRepo.isNullOrBlank()
    var modIssue: String? = null
    fun hasIssue() = !modIssue.isNullOrBlank()
    var fabricApi = true
    var architecturyApi = true
    var mixins = false
    var license: License? = null

    override var type = PlatformType.ARCHITECTURY

    override val preferredBuildSystem = BuildSystemType.GRADLE

    override val javaVersion: JavaVersion
        get() = MinecraftVersions.requiredJavaVersion(mcVersion)

    override val compatibleGradleVersions: VersionRange
        get() = VersionRange.fixed(gradleVersion)

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return ArchitecturyProjectCreator(
            rootDirectory,
            module,
            buildSystem,
            this
        )
    }
}
