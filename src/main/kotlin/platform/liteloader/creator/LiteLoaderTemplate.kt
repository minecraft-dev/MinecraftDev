/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.LITELOADER_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.LITELOADER_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.LITELOADER_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.LITELOADER_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.project.Project

object LiteLoaderTemplate : BaseTemplate() {

    fun applyMainClass(
        project: Project,
        packageName: String,
        className: String,
        modName: String,
        modVersion: String
    ): String {
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "CLASS_NAME" to className,
            "MOD_NAME" to modName,
            "MOD_VERSION" to modVersion
        )

        return project.applyTemplate(LITELOADER_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem, mcVersion: SemanticVersion): String {
        val props = mapOf(
            "FORGEGRADLE_VERSION" to fgVersion(mcVersion),
            "GROUP_ID" to buildSystem.groupId,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "VERSION" to buildSystem.version
        )

        return project.applyTemplate(LITELOADER_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project, config: LiteLoaderProjectConfig): String {
        val props = mapOf(
            "MC_VERSION" to config.mcVersion.toString(),
            "MCP_MAPPINGS" to config.mcpVersion.mcpVersion
        )

        return project.applyTemplate(LITELOADER_GRADLE_PROPERTIES_TEMPLATE, props)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(LITELOADER_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(project: Project, buildSystem: BuildSystem, mcVersion: SemanticVersion): String {
        val props = mapOf(
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName,
            "FORGEGRADLE_VERSION" to fgVersion(mcVersion),
            "ARTIFACT_ID" to buildSystem.artifactId
        )

        return project.applyTemplate(LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }

    private fun fgVersion(mcVersion: SemanticVersion): String {
        // Fixes builds for MC1.12+, requires FG 2.3
        return if (mcVersion >= ForgeModuleType.FG23_MC_VERSION) {
            "2.3"
        } else {
            "2.2"
        }
    }
}
