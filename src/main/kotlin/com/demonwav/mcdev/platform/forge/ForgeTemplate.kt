/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.hybrid.SpongeForgeProjectConfiguration
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object ForgeTemplate {
    val MC_1_12 = SemanticVersion.release(1, 12)

    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        groupId: String,
        artifactId: String,
        configuration: ForgeProjectConfiguration,
        modVersion: String
    ) {

        val properties = Properties()

        applyForgeGradle(properties, configuration)

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FORGE_BUILD_GRADLE_TEMPLATE,
            properties
        )

        val gradleProps = Properties()
        gradleProps.setProperty("GROUP_ID", groupId)
        gradleProps.setProperty("ARTIFACT_ID", artifactId)
        gradleProps.setProperty("MOD_VERSION", modVersion)
        gradleProps.setProperty("FORGE_VERSION", configuration.forgeVersion)
        gradleProps.setProperty("MCP_VERSION", configuration.mcpVersion)

        // create gradle.properties
        BaseTemplate.applyTemplate(
            project,
            prop,
            MinecraftFileTemplateGroupFactory.FORGE_GRADLE_PROPERTIES_TEMPLATE,
            gradleProps
        )
    }

    fun applySubmoduleBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        artifactId: String,
        configuration: ForgeProjectConfiguration,
        commonProjectName: String
    ) {

        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        applyForgeGradle(properties, configuration)

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE,
            properties
        )

        val gradleProps = Properties()
        gradleProps.setProperty("ARTIFACT_ID", artifactId)
        gradleProps.setProperty("FORGE_VERSION", configuration.forgeVersion)
        gradleProps.setProperty("MCP_VERSION", configuration.mcpVersion)

        // create gradle.properties
        BaseTemplate.applyTemplate(
            project,
            prop,
            MinecraftFileTemplateGroupFactory.FORGE_GRADLE_PROPERTIES_TEMPLATE,
            gradleProps
        )
    }

    private fun applyForgeGradle(properties: Properties, configuration: ForgeProjectConfiguration) {
        if (configuration is SpongeForgeProjectConfiguration) {
            properties.setProperty("SPONGE_FORGE", "true")
        }
        properties.setProperty("FORGEGRADLE_VERSION", "2.3")
    }

    fun applyMcmodInfoTemplate(
        project: Project,
        file: VirtualFile,
        artifactId: String,
        modName: String,
        description: String,
        url: String,
        updateUrl: String,
        authorList: String?,
        dependenciesList: String?
    ) {
        val properties = Properties()
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("DESCRIPTION", description)
        properties.setProperty("URL", url)
        properties.setProperty("UPDATE_URL", updateUrl)

        if (!authorList.isNullOrBlank()) {
            properties.setProperty("HAS_AUTHOR_LIST", "true")
            properties.setProperty("AUTHOR_LIST", authorList)
        }

        if (!dependenciesList.isNullOrBlank()) {
            properties.setProperty("HAS_DEPENDENCIES_LIST", "true")
            properties.setProperty("DEPENDENCIES_LIST", dependenciesList)
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.MCMOD_INFO_TEMPLATE, properties)
    }

    fun applyMainClassTemplate(
        project: Project,
        file: VirtualFile,
        packageName: String,
        artifactId: String,
        modName: String,
        modVersion: String,
        className: String
    ) {
        val properties = Properties()
        properties.setProperty("PACKAGE_NAME", packageName)
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("MOD_VERSION", modVersion)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.FORGE_MAIN_CLASS_TEMPLATE,
            properties
        )
    }
}
