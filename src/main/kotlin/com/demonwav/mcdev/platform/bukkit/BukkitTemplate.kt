/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object BukkitTemplate {

    fun applyMainClassTemplate(project: Project,
                               file: VirtualFile,
                               packageName: String,
                               className: String) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE, properties)
    }

    fun applyPomTemplate(project: Project,
                         version: String): String {
        val properties = Properties()
        properties.setProperty("BUILD_VERSION", version)

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }

    fun applyPluginDescriptionFileTemplate(project: Project,
                                           file: VirtualFile,
                                           settings: BukkitProjectConfiguration,
                                           buildSystem: BuildSystem) {
        val properties = Properties()

        properties.setProperty("NAME", settings.pluginName)

        if (buildSystem is GradleBuildSystem) {
            properties.setProperty("VERSION", "@version@")
        } else if (buildSystem is MavenBuildSystem) {
            properties.setProperty("VERSION", "\${project.version}")
        }

        properties.setProperty("MAIN", settings.mainClass)

        if (settings.hasPrefix()) {
            properties.setProperty("PREFIX", settings.prefix)
            properties.setProperty("HAS_PREFIX", "true")
        }

        if (settings.loadOrder != LoadOrder.POSTWORLD) {
            properties.setProperty("LOAD", LoadOrder.STARTUP.name)
            properties.setProperty("HAS_LOAD", "true")
        }

        if (settings.hasLoadBefore()) {
            properties.setProperty("LOAD_BEFORE", settings.loadBefore.toString())
            properties.setProperty("HAS_LOAD_BEFORE", "true")
        }

        if (settings.hasDependencies()) {
            properties.setProperty("DEPEND", settings.dependencies.toString())
            properties.setProperty("HAS_DEPEND", "true")
        }

        if (settings.hasSoftDependencies()) {
            properties.setProperty("SOFT_DEPEND", settings.softDependencies.toString())
            properties.setProperty("HAS_SOFT_DEPEND", "true")
        }

        if (settings.hasAuthors()) {
            properties.setProperty("AUTHOR_LIST", settings.authors.toString())
            properties.setProperty("HAS_AUTHOR_LIST", "true")
        }

        if (settings.hasDescription()) {
            properties.setProperty("DESCRIPTION", settings.description)
            properties.setProperty("HAS_DESCRIPTION", "true")
        }

        if (settings.hasWebsite()) {
            properties.setProperty("WEBSITE", settings.website)
            properties.setProperty("HAS_WEBSITE", "true")
        }

        // Plugins targeting 1.13 or newer need an explicit api declaration flag
        // Unfortunately this flag has no contract to match any specific API version
        val mcVer = settings.minecraftVersion.substring(0, 4).toDoubleOrNull()
        if (mcVer != null && mcVer >= 1.13) {
            properties.setProperty("API_VERSION", "1.13")
            properties.setProperty("HAS_API_VERSION", "true")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties, true)
    }
}
