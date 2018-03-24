/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object BungeeCordTemplate {

    fun applyMainClassTemplate(project: Project,
                               file: VirtualFile,
                               packageName: String,
                               className: String) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUNGEECORD_MAIN_CLASS_TEMPLATE, properties)
    }

    fun applyPomTemplate(project: Project,
                         version: String): String {
        val properties = Properties()
        properties.setProperty("BUILD_VERSION", version)

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUNGEECORD_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }

    fun applyPluginDescriptionFileTemplate(project: Project,
                                           file: VirtualFile,
                                           settings: BungeeCordProjectConfiguration,
                                           buildSystem: BuildSystem) {
        val properties = Properties()

        properties.setProperty("NAME", settings.pluginName)

        if (buildSystem is GradleBuildSystem) {
            properties.setProperty("VERSION", "@version@")
        } else if (buildSystem is MavenBuildSystem) {
            properties.setProperty("VERSION", "\${project.version}")
        }

        properties.setProperty("MAIN", settings.mainClass)

        if (settings.hasDependencies()) {
            properties.setProperty("DEPEND", settings.dependencies.toString())
            properties.setProperty("HAS_DEPEND", "true")
        }

        if (settings.hasSoftDependencies()) {
            properties.setProperty("SOFT_DEPEND", settings.softDependencies.toString())
            properties.setProperty("HAS_SOFT_DEPEND", "true")
        }

        if (settings.hasAuthors()) {
            // BungeeCord only supports one author
            properties.setProperty("AUTHOR", settings.authors[0])
            properties.setProperty("HAS_AUTHOR", "true")
        }

        if (settings.hasDescription()) {
            properties.setProperty("DESCRIPTION", settings.description)
            properties.setProperty("HAS_DESCRIPTION", "true")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE, properties, true)
    }
}
