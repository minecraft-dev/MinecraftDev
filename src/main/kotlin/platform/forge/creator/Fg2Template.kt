/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FORGE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FORGE_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FORGE_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FORGE_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MCMOD_INFO_TEMPLATE
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.project.Project

object Fg2Template : BaseTemplate() {

    fun applyMainClass(
        project: Project,
        buildSystem: BuildSystem,
        config: ForgeProjectConfig,
        packageName: String,
        className: String
    ): String {
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "CLASS_NAME" to className,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "MOD_VERSION" to buildSystem.version
        )

        return project.applyTemplate(FORGE_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem, mcVersion: SemanticVersion): String {
        val props = mapOf(
            "FORGEGRADLE_VERSION" to fgVersion(mcVersion),
            "GROUP_ID" to buildSystem.groupId,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_VERSION" to buildSystem.version
        )

        return project.applyTemplate(FORGE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(
        project: Project,
        config: ForgeProjectConfig
    ): String {
        val props = mapOf(
            "FORGE_VERSION" to config.forgeVersionText,
            "MCP_VERSION" to config.mcpVersion.mcpVersion
        )

        return project.applyTemplate(FORGE_GRADLE_PROPERTIES_TEMPLATE, props)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(FORGE_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(project: Project, buildSystem: BuildSystem, mcVersion: SemanticVersion): String {
        val props = mapOf(
            "FORGEGRADLE_VERSION" to fgVersion(mcVersion),
            "ARTIFACT_ID" to buildSystem.artifactId,
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName
        )

        return project.applyTemplate(FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyMcmodInfo(project: Project, buildSystem: BuildSystem, config: ForgeProjectConfig): String {
        val props = mutableMapOf(
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName
        )
        props["DESCRIPTION"] = config.description ?: ""
        config.website?.let { url ->
            if (url.isNotBlank()) {
                props["URL"] = url
            }
        }
        config.updateUrl?.let { url ->
            if (url.isNotBlank()) {
                props["UPDATE_URL"] = url
            }
        }
        if (config.hasAuthors()) {
            props["AUTHOR_LIST"] = config.authors.joinToString(", ") { "\"$it\"" }
        }

        return project.applyTemplate(MCMOD_INFO_TEMPLATE, props)
    }

    private fun fgVersion(mcVersion: SemanticVersion): String {
        return if (mcVersion >= ForgeModuleType.FG23_MC_VERSION) {
            "2.3"
        } else {
            "2.2"
        }
    }
}
