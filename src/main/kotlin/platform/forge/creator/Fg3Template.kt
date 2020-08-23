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

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FG3_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FG3_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FG3_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FG3_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.FG3_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MODS_TOML_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.PACK_MCMETA_TEMPLATE
import com.intellij.openapi.project.Project

object Fg3Template : BaseTemplate() {

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

        return project.applyTemplate(FG3_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ForgeProjectConfig,
        modName: String,
        hasData: Boolean
    ): String {
        val (channel, version) = config.mcpVersion.mcpVersion.split('_', limit = 2)
        val props = mutableMapOf(
            "MOD_NAME" to modName,
            "MCP_CHANNEL" to channel,
            "MCP_VERSION" to version,
            "MCP_MC_VERSION" to config.mcpVersion.mcVersion.toString(),
            "FORGE_VERSION" to config.forgeVersionText,
            "GROUP_ID" to buildSystem.groupId,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_VERSION" to buildSystem.version
        )
        if (hasData) {
            props["HAS_DATA"] = "true"
        }

        return project.applyTemplate(FG3_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String {
        return project.applyTemplate(FG3_GRADLE_PROPERTIES_TEMPLATE)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(FG3_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ForgeProjectConfig,
        modName: String,
        hasData: Boolean
    ): String {
        val (channel, version) = config.mcpVersion.mcpVersion.split('_', limit = 2)
        val props = mutableMapOf(
            "MOD_NAME" to modName,
            "MCP_CHANNEL" to channel,
            "MCP_VERSION" to version,
            "MCP_MC_VERSION" to config.mcpVersion.mcVersion.toString(),
            "FORGE_VERSION" to config.forgeVersionText,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName
        )
        if (hasData) {
            props["HAS_DATA"] = "true"
        }

        return project.applyTemplate(FG3_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyModsToml(project: Project, buildSystem: BuildSystem, config: ForgeProjectConfig): String {
        val props = mutableMapOf(
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName
        )
        props["DESCRIPTION"] = config.description ?: ""
        config.updateUrl?.let { url ->
            if (url.isNotBlank()) {
                props["UPDATE_URL"] = url
            }
        }
        if (config.hasAuthors()) {
            props["AUTHOR_LIST"] = config.authors.joinToString(", ")
        }

        return project.applyTemplate(MODS_TOML_TEMPLATE, props)
    }

    fun applyPackMcmeta(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(PACK_MCMETA_TEMPLATE, props)
    }
}
