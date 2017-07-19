/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.liteloader

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object LiteLoaderTemplate {

    fun applyBuildGradleTemplate(project: Project,
                                 file: VirtualFile,
                                 prop: VirtualFile,
                                 groupId: String,
                                 artifactId: String,
                                 configuration: LiteLoaderProjectConfiguration) {

        val properties = Properties()
        properties.setProperty("GROUP_ID", groupId)
        properties.setProperty("ARTIFACT_ID", artifactId)
        properties.setProperty("VERSION", configuration.pluginVersion)
        properties.setProperty("MC_VERSION", configuration.mcVersion)
        properties.setProperty("MCP_MAPPINGS", configuration.mcpVersion)

        BaseTemplate.applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.LITELOADER_GRADLE_PROPERTIES_TEMPLATE, properties)
        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_BUILD_GRADLE_TEMPLATE, Properties())
    }

    fun applySubmoduleBuildGradleTemplate(project: Project,
                                          file: VirtualFile,
                                          prop: VirtualFile,
                                          configuration: LiteLoaderProjectConfiguration,
                                          commonProjectName: String) {

        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE, properties)

        val gradleProps = Properties()
        gradleProps.setProperty("VERSION", configuration.pluginVersion)
        gradleProps.setProperty("MC_VERSION", configuration.mcVersion)
        gradleProps.setProperty("MCP_MAPPINGS", configuration.mcpVersion)

        BaseTemplate.applyTemplate(project, prop, MinecraftFileTemplateGroupFactory.LITELOADER_GRADLE_PROPERTIES_TEMPLATE, gradleProps)
    }

    fun applyMainClassTemplate(project: Project,
                               file: VirtualFile,
                               packageName: String,
                               className: String,
                               modName: String,
                               modVersion: String) {

        val properties = Properties()
        properties.setProperty("PACKAGE_NAME", packageName)
        properties.setProperty("CLASS_NAME", className)
        properties.setProperty("MOD_NAME", modName)
        properties.setProperty("MOD_VERSION", modVersion)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.LITELOADER_MAIN_CLASS_TEMPLATE, properties)
    }
}
