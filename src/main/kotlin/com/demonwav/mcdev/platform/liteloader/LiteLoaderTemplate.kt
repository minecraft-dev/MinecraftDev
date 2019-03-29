/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.forge.ForgeTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object LiteLoaderTemplate {

    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        groupId: String,
        artifactId: String,
        configuration: LiteLoaderProjectConfiguration
    ) {
        val properties = Properties()
        properties.setProperty("GROUP_ID", groupId)
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("VERSION", configuration.base?.pluginVersion)
        properties.setProperty("MC_VERSION", configuration.mcVersion)
        properties.setProperty("MCP_MAPPINGS", configuration.mcpVersion)

        BaseTemplate.applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.LITELOADER_GRADLE_PROPERTIES_TEMPLATE, properties)

        val gradleProps = Properties()

        // Fixes builds for MC1.12+, requires FG 2.3
        val mcVersion = SemanticVersion.parse(configuration.mcVersion)
        if (mcVersion >= ForgeTemplate.MC_1_12) {
            gradleProps.setProperty("FORGEGRADLE_VERSION", "2.3")
        } else {
            gradleProps.setProperty("FORGEGRADLE_VERSION", "2.2")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_BUILD_GRADLE_TEMPLATE, gradleProps)
    }

    fun applySubmoduleBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        configuration: LiteLoaderProjectConfiguration,
        commonProjectName: String
    ) {
        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        // Fixes builds for MC1.12+, requires FG 2.3
        val mcVersion = SemanticVersion.parse(configuration.mcVersion)
        if (mcVersion >= ForgeTemplate.MC_1_12) {
            properties.setProperty("FORGEGRADLE_VERSION", "2.3")
        } else {
            properties.setProperty("FORGEGRADLE_VERSION", "2.2")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE, properties)

        val gradleProps = Properties()
        gradleProps.setProperty("VERSION", configuration.base?.pluginVersion)
        gradleProps.setProperty("MC_VERSION", configuration.mcVersion)
        gradleProps.setProperty("MCP_MAPPINGS", configuration.mcpVersion)

        BaseTemplate.applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.LITELOADER_GRADLE_PROPERTIES_TEMPLATE, gradleProps)
    }

    fun applyMainClassTemplate(
        project: Project,
        file: VirtualFile,
        packageName: String,
        className: String,
        modName: String,
        modVersion: String
    ) {
        val properties = Properties()
        properties.setProperty("PACKAGE_NAME", packageName)
        properties.setProperty("CLASS_NAME", className)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("MOD_VERSION", modVersion)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_MAIN_CLASS_TEMPLATE, properties)
    }
}
