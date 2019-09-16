/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.ProjectCreator
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleCreator
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.mcp.McpVersionPair
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.module.Module
import java.nio.file.Path

class ForgeProjectConfig : ProjectConfig(), GradleCreator {

    lateinit var mainClass: String

    var updateUrl: String? = null

    override var type: PlatformType = PlatformType.FORGE

    var mcpVersion = McpVersionPair("", SemanticVersion.release())
    var forgeVersionText: String = ""
    var forgeVersion: SemanticVersion = SemanticVersion.release()
    var mcVersion: SemanticVersion = SemanticVersion.release()

    override val preferredBuildSystem = BuildSystemType.GRADLE

    override fun buildGradleCreator(
        rootDirectory: Path,
        module: Module,
        buildSystem: GradleBuildSystem
    ): ProjectCreator {
        return if (isFg3(mcVersion, forgeVersion)) {
            if (mcVersion >= ForgeModuleType.FG3_MC_VERSION) {
                Fg3ProjectCreator(rootDirectory, module, buildSystem, this)
            } else {
                Fg3Mc112ProjectCreator(rootDirectory, module, buildSystem, this)
            }
        } else {
            Fg2ProjectCreator(rootDirectory, module, buildSystem, this, mcVersion)
        }
    }

    override fun configureRootGradle(
        rootDirectory: Path,
        buildSystem: GradleBuildSystem
    ) {
        if (!isFg3(mcVersion, forgeVersion)) {
            buildSystem.gradleVersion = Fg2ProjectCreator.FG2_WRAPPER_VERSION
        }
    }

    private fun isFg3(mcVersion: SemanticVersion, forgeVersion: SemanticVersion): Boolean {
        return mcVersion >= ForgeModuleType.FG3_MC_VERSION || forgeVersion >= ForgeModuleType.FG3_FORGE_VERSION
    }
}
