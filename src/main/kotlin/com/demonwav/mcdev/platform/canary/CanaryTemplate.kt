/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object CanaryTemplate {

    fun applyMainClassTemplate(project: Project,
                               file: VirtualFile,
                               packageName: String,
                               className: String) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.CANARY_MAIN_CLASS_TEMPLATE, properties)
    }

    fun applyPomTemplate(project: Project,
                         version: String): String {
        val properties = Properties()
        properties.setProperty("BUILD_VERSION", version)

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.CANARY_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }

    fun applyPluginDescriptionFileTemplate(project: Project,
                                           file: VirtualFile,
                                           settings: CanaryProjectConfiguration,
                                           buildSystem: BuildSystem) {
        val properties = Properties()

        properties.setProperty("NAME", settings.pluginName)

        if (buildSystem is GradleBuildSystem) {
            properties.setProperty("VERSION", "@version@")
        } else if (buildSystem is MavenBuildSystem) {
            properties.setProperty("VERSION", "\${project.version}")
        }

        properties.setProperty("MAIN_CLASS", settings.mainClass)

        if (settings.hasAuthors()) {
            properties.setProperty("AUTHOR_LIST", settings.authors.toString())
            properties.setProperty("HAS_AUTHOR_LIST", "true")
        }

        if (settings.isEnableEarly) {
            properties.setProperty("ENABLE_EARLY", "true")
        }

        if (settings.hasDependencies()) {
            properties.setProperty("DEPEND", settings.dependencies.toString())
            properties.setProperty("HAS_DEPEND", "true")
        }

        BaseTemplate.applyTemplate(project, file, MinecraftFileTemplateGroupFactory.CANARY_INF_TEMPLATE, properties, true)
    }
}
