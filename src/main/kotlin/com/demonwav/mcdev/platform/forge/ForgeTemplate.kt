/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory.Companion.FG3_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory.Companion.FORGE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory.Companion.FORGE_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object ForgeTemplate {
    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        buildSystem: BuildSystem,
        configuration: ForgeProjectConfiguration,
        modName: String
    ) {
        val mcVersion = SemanticVersion.parse(configuration.mcVersion)
        val isFg2 = mcVersion < ForgeModuleType.FG3_VERSION
        if (isFg2) {
            BaseTemplate.applyTemplate(
                project, file, FORGE_BUILD_GRADLE_TEMPLATE,
                Properties().apply {
                    if (mcVersion >= ForgeModuleType.FG2_3_VERSION) {
                        setProperty("FORGEGRADLE_VERSION", "2.3")
                    } else {
                        setProperty("FORGEGRADLE_VERSION", "2.2")
                    }
                }
            )
        } else {
            BaseTemplate.applyTemplate(
                project, file, FG3_BUILD_GRADLE_TEMPLATE,
                Properties().apply {
                    setProperty("MOD_NAME", modName)
                    val (channel, version) = configuration.mcpVersion.mcpVersion.split('_', limit = 2)
                    setProperty("MCP_CHANNEL", channel)
                    setProperty("MCP_VERSION", version)
                    setProperty("MCP_MC_VERSION", configuration.mcpVersion.mcVersion)
                    setProperty("FORGE_VERSION", configuration.forgeVersion)
                }
            )
        }

        val properties = Properties()
        properties.setProperty("GROUP_ID", buildSystem.groupId)
        properties.setProperty("ARTIFACT_ID", buildSystem.artifactId)
        properties.setProperty("MOD_VERSION", buildSystem.version)
        if (isFg2) {
            properties.setProperty("FORGE_VERSION", configuration.forgeVersion)
            properties.setProperty("MCP_VERSION", configuration.mcpVersion.mcpVersion)
        }

        // create gradle.properties
        BaseTemplate.applyTemplate(project, prop, FORGE_GRADLE_PROPERTIES_TEMPLATE, properties)
    }

    fun applySubmoduleBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        artifactId: String,
        configuration: ForgeProjectConfiguration,
        commonProjectName: String,
        modName: String
    ) {
        val mcVersion = SemanticVersion.parse(configuration.mcVersion)
        val isFg2 = mcVersion < ForgeModuleType.FG3_VERSION

        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        val template = if (isFg2) {
            if (mcVersion >= ForgeModuleType.FG2_3_VERSION) {
                properties.setProperty("FORGEGRADLE_VERSION", "2.3")
            } else {
                properties.setProperty("FORGEGRADLE_VERSION", "2.2")
            }
            MinecraftFileTemplateGroupFactory.FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE
        } else {
            properties.setProperty("MOD_NAME", modName)
            val (channel, version) = configuration.mcpVersion.mcpVersion.split('_', limit = 2)
            properties.setProperty("MCP_CHANNEL", channel)
            properties.setProperty("MCP_VERSION", version)
            properties.setProperty("MCP_MC_VERSION", configuration.mcpVersion.mcVersion)
            properties.setProperty("FORGE_VERSION", configuration.forgeVersion)
            MinecraftFileTemplateGroupFactory.FG3_SUBMODULE_BUILD_GRADLE_TEMPLATE
        }

        BaseTemplate.applyTemplate(project, file, template, properties)

        val gradleProps = Properties()
        gradleProps.setProperty("ARTIFACT_ID", artifactId)
        if (isFg2) {
            gradleProps.setProperty("FORGE_VERSION", configuration.forgeVersion)
            gradleProps.setProperty("MCP_VERSION", configuration.mcpVersion.mcpVersion)
        }

        // create gradle.properties
        BaseTemplate.applyTemplate(
            project,
            prop,
            MinecraftFileTemplateGroupFactory.FORGE_GRADLE_PROPERTIES_TEMPLATE,
            gradleProps
        )
    }

    fun applyModDescriptorTemplate(
        project: Project,
        file: VirtualFile,
        artifactId: String,
        modName: String,
        description: String,
        url: String?,
        updateUrl: String?,
        authorList: String?,
        mcVersion: SemanticVersion
    ) {
        val properties = Properties()
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("DESCRIPTION", description)
        if (!url.isNullOrBlank()) {
            properties.setProperty("URL", url)
        }
        if (!updateUrl.isNullOrBlank()) {
            properties.setProperty("UPDATE_URL", updateUrl)
        }

        if (!authorList.isNullOrBlank()) {
            properties.setProperty("HAS_AUTHOR_LIST", "true")
            properties.setProperty("AUTHOR_LIST", authorList)
        }

        val template = if (mcVersion < ForgeModuleType.FG3_VERSION) {
            MinecraftFileTemplateGroupFactory.MCMOD_INFO_TEMPLATE
        } else {
            MinecraftFileTemplateGroupFactory.MODS_TOML_TEMPLATE
        }
        BaseTemplate.applyTemplate(project, file, template, properties)
    }

    fun applyPackMcmetaTemplate(
        project: Project,
        file: VirtualFile,
        artifactId: String
    ) {
        val properties = Properties()
        properties.setProperty("ARTIFACT_ID", artifactId)
        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.PACK_MCMETA_TEMPLATE, properties)
    }

    fun applyMainClassTemplate(
        project: Project,
        file: VirtualFile,
        packageName: String,
        artifactId: String,
        modName: String,
        modVersion: String,
        className: String,
        mcVersion: SemanticVersion
    ) {
        val properties = Properties()
        properties.setProperty("PACKAGE_NAME", packageName)
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("MOD_VERSION", modVersion)
        properties.setProperty("CLASS_NAME", className)

        val template = if (mcVersion < ForgeModuleType.FG3_VERSION) {
            MinecraftFileTemplateGroupFactory.FORGE_MAIN_CLASS_TEMPLATE
        } else {
            MinecraftFileTemplateGroupFactory.FG3_MAIN_CLASS_TEMPLATE
        }

        BaseTemplate.applyTemplate(project, file, template, properties)
    }
}
