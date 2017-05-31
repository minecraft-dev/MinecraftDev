/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object ForgeTemplate {

    fun applyBuildGradleTemplate(project: Project,
                                 file: VirtualFile,
                                 prop: VirtualFile,
                                 groupId: String,
                                 artifactId: String,
                                 forgeVersion: String,
                                 mcpVersion: String,
                                 modVersion: String,
                                 spongeForge: Boolean) {

        val properties = Properties()

        if (spongeForge) {
            properties.setProperty("SPONGE_FORGE", "true")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.FORGE_BUILD_GRADLE_TEMPLATE, properties)

        val gradleProps = Properties()
        gradleProps.setProperty("GROUP_ID", groupId)
        gradleProps.setProperty("ARTIFACT_ID", artifactId)
        gradleProps.setProperty("MOD_VERSION", modVersion)
        gradleProps.setProperty("FORGE_VERSION", forgeVersion)
        gradleProps.setProperty("MCP_VERSION", mcpVersion)

        // create gradle.properties
        BaseTemplate.applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.FORGE_GRADLE_PROPERTIES_TEMPLATE, gradleProps)
    }

    fun applySubmoduleBuildGradleTemplate(project: Project,
                                          file: VirtualFile,
                                          prop: VirtualFile,
                                          artifactId: String,
                                          forgeVersion: String,
                                          mcpVersion: String,
                                          commonProjectName: String,
                                          spongeForge: Boolean) {

        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        if (spongeForge) {
            properties.setProperty("SPONGE_FORGE", "true")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, properties)

        val gradleProps = Properties()
        gradleProps.setProperty("ARTIFACT_ID", artifactId)
        gradleProps.setProperty("FORGE_VERSION", forgeVersion)
        gradleProps.setProperty("MCP_VERSION", mcpVersion)

        // create gradle.properties
        BaseTemplate.applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.FORGE_GRADLE_PROPERTIES_TEMPLATE, gradleProps)
    }

    fun applyMcmodInfoTemplate(project: Project,
                               file: VirtualFile,
                               artifactId: String,
                               modName: String,
                               description: String,
                               url: String,
                               updateUrl: String,
                               authorList: String?,
                               dependenciesList: String?) {

        val properties = Properties()
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("DESCRIPTION", description)
        properties.setProperty("URL", url)
        properties.setProperty("UPDATE_URL", updateUrl)

        if (authorList != null) {
            properties.setProperty("HAS_AUTHOR_LIST", "true")
            properties.setProperty("AUTHOR_LIST", authorList)
        }

        if (dependenciesList != null) {
            properties.setProperty("HAS_DEPENDENCIES_LIST", "true")
            properties.setProperty("DEPENDENCIES_LIST", dependenciesList)
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.MCMOD_INFO_TEMPLATE, properties)
    }

    fun applyMainClassTemplate(project: Project,
                               file: VirtualFile,
                               packageName: String,
                               artifactId: String,
                               modName: String,
                               modVersion: String,
                               className: String) {

        val properties = Properties()
        properties.setProperty("PACKAGE_NAME", packageName)
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("MOD_VERSION", modVersion)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.FORGE_MAIN_CLASS_TEMPLATE, properties)
    }
}
