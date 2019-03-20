/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object SpongeTemplate {

    fun applyPomTemplate(project: Project): String {

        val properties = Properties()

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }

    fun applyMainClassTemplate(
        project: Project,
        mainClassFile: VirtualFile,
        packageName: String,
        className: String,
        hasDependencies: Boolean
    ) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)
        if (hasDependencies) {
            properties.setProperty("HAS_DEPENDENCIES", "true")
        }

        BaseTemplate.applyTemplate(
            project,
            mainClassFile,
            MinecraftFileTemplateGroupFactory.SPONGE_MAIN_CLASS_TEMPLATE,
            properties
        )
    }

    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        groupId: String,
        artifactId: String,
        pluginVersion: String
    ): String? {
        val properties = Properties()

        val manager = FileTemplateManager.getInstance(project)
        val template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_BUILD_GRADLE_TEMPLATE)

        // Sponge \o/
        BaseTemplate.applyGradlePropertiesTemplate(project, file, groupId, artifactId, pluginVersion, true)

        return template.getText(properties)
    }

    fun applySubmoduleBuildGradleTemplate(
        project: Project,
        commonProjectName: String
    ): String? {
        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        val manager = FileTemplateManager.getInstance(project)
        val template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE)

        return template.getText(properties)
    }
}
