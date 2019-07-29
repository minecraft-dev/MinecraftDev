/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.data.LoadOrder
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object BukkitTemplate {

    fun applyMainClassTemplate(
        project: Project,
        file: VirtualFile,
        packageName: String,
        className: String
    ) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.BUKKIT_MAIN_CLASS_TEMPLATE,
            properties
        )
    }

    fun applyPomTemplate(project: Project): String {
        val properties = Properties()

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUKKIT_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }

    fun applyPluginDescriptionFileTemplate(
        project: Project,
        file: VirtualFile,
        config: BukkitProjectConfiguration,
        buildSystem: BuildSystem
    ) {
        val base = config.base ?: return
        val data = config.data ?: return

        val properties = bukkitMain(buildSystem, base)

        if (config.hasPrefix()) {
            properties.setProperty("PREFIX", data.prefix)
            properties.setProperty("HAS_PREFIX", "true")
        }

        if (data.loadOrder != LoadOrder.POSTWORLD) {
            properties.setProperty("LOAD", LoadOrder.STARTUP.name)
            properties.setProperty("HAS_LOAD", "true")
        }

        if (config.hasLoadBefore()) {
            properties.setProperty("LOAD_BEFORE", data.loadBefore.toString())
            properties.setProperty("HAS_LOAD_BEFORE", "true")
        }

        bukkitDeps(properties, config)

        if (config.hasAuthors()) {
            properties.setProperty("AUTHOR_LIST", base.authors.toString())
            properties.setProperty("HAS_AUTHOR_LIST", "true")
        }

        if (config.hasDescription()) {
            properties.setProperty("DESCRIPTION", base.description)
            properties.setProperty("HAS_DESCRIPTION", "true")
        }

        if (config.hasWebsite()) {
            properties.setProperty("WEBSITE", base.website)
            properties.setProperty("HAS_WEBSITE", "true")
        }

        // Plugins targeting 1.13 or newer need an explicit api declaration flag
        // Unfortunately this flag has no contract to match any specific API version
        if (data.minecraftVersion.length >= 4) {
            val mcVer = data.minecraftVersion.substring(0, 4).toDoubleOrNull()
            if (mcVer != null && mcVer >= 1.13) {
                properties.setProperty("API_VERSION", "1.13")
                properties.setProperty("HAS_API_VERSION", "true")
            }
        }

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE,
            properties,
            true
        )
    }

    fun bukkitMain(buildSystem: BuildSystem, base: ProjectConfiguration.BaseConfigs): Properties {
        val properties = Properties()

        properties.setProperty("NAME", base.pluginName)

        if (buildSystem is GradleBuildSystem) {
            properties.setProperty("VERSION", "@version@")
        } else if (buildSystem is MavenBuildSystem) {
            properties.setProperty("VERSION", "\${project.version}")
        }

        properties.setProperty("MAIN", base.mainClass)

        return properties
    }

    fun bukkitDeps(properties: Properties, configuration: BukkitLikeConfiguration) {
        if (configuration.hasDependencies()) {
            properties.setProperty("DEPEND", configuration.dependencies.toString())
            properties.setProperty("HAS_DEPEND", "true")
        }

        if (configuration.hasSoftDependencies()) {
            properties.setProperty("SOFT_DEPEND", configuration.softDependencies.toString())
            properties.setProperty("HAS_SOFT_DEPEND", "true")
        }
    }
}
