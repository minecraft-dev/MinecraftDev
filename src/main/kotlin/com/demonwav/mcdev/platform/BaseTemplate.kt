/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform

import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.util.Locale
import java.util.Properties

object BaseTemplate {

    private val NEW_LINE = Regex("\\n+")

    fun applyBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        groupId: String,
        pluginVersion: String
    ): String? {
        val manager = FileTemplateManager.getInstance(project)
        val template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUILD_GRADLE_TEMPLATE)

        applyGradlePropertiesTemplate(project, file, groupId, pluginVersion)

        return template.getText(manager.defaultProperties)
    }

    fun applyMultiModuleBuildGradleTemplate(
        project: Project,
        file: VirtualFile,
        prop: VirtualFile,
        groupId: String,
        pluginVersion: String,
        artifactId: String? = null
    ) {
        val properties = Properties()

        applyGradlePropertiesTemplate(
            project,
            prop,
            groupId,
            pluginVersion,
            artifactId
        )

        applyTemplate(project, file, MinecraftFileTemplateGroupFactory.MULTI_MODULE_BUILD_GRADLE_TEMPLATE, properties)
    }

    fun applyGradlePropertiesTemplate(
        project: Project,
        file: VirtualFile,
        groupId: String,
        pluginVersion: String,
        artifactId: String? = null
    ) {
        val gradleProps = Properties()

        gradleProps.setProperty("GROUP_ID", groupId)
        gradleProps.setProperty("PLUGIN_VERSION", pluginVersion)

        if (!artifactId.isNullOrBlank()) {
            gradleProps.setProperty("PLUGIN_ID", artifactId.toLowerCase(Locale.ENGLISH))
        }

        // create gradle.properties
        applyTemplate(project, file, MinecraftFileTemplateGroupFactory.GRADLE_PROPERTIES_TEMPLATE, gradleProps)
    }

    fun applySettingsGradleTemplate(
        project: Project,
        file: VirtualFile,
        projectName: String,
        includes: String
    ) {
        val properties = Properties()
        properties.setProperty("PROJECT_NAME", projectName)
        properties.setProperty("INCLUDES", includes)

        applyTemplate(project, file, MinecraftFileTemplateGroupFactory.SETTINGS_GRADLE_TEMPLATE, properties)
    }

    fun applySubmoduleBuildGradleTemplate(
        project: Project,
        commonProjectName: String
    ): String? {
        val properties = Properties()
        properties.setProperty("COMMON_PROJECT_NAME", commonProjectName)

        val manager = FileTemplateManager.getInstance(project)
        val template = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.SUBMODULE_BUILD_GRADLE_TEMPLATE)

        return template.getText(properties)
    }

    fun applyTemplate(
        project: Project,
        file: VirtualFile,
        templateName: String,
        properties: Properties? = null,
        trimNewlines: Boolean = false
    ) {
        val manager = FileTemplateManager.getInstance(project)
        val template = manager.getJ2eeTemplate(templateName)

        val allProperties = manager.defaultProperties
        properties?.let { prop -> allProperties.putAll(prop) }

        var text = template.getText(allProperties)
        if (trimNewlines) {
            text = text.replace(NEW_LINE, "\n")
        }
        VfsUtil.saveText(file, text)

        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (psiFile != null) {
            ReformatCodeProcessor(project, psiFile, null, false).run()
        }
    }
}
