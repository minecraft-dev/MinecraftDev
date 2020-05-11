/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.placeholderapi

import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object PlaceholderApiTemplate {

    fun applyMainClassTemplate(
        project: Project,
        mainClassFile: VirtualFile,
        packageName: String,
        className: String,
        expansionName: String,
        expansionVersion: String,
        author: MutableList<String>
    ) {
        val properties = Properties()

        properties.setProperty("PACKAGE", packageName)
        properties.setProperty("CLASS_NAME", className)
        properties.setProperty("EXPANSION_NAME", expansionName)
        properties.setProperty("EXPANSION_VERSION", expansionVersion)
        properties.setProperty("EXPANSION_AUTHOR", author[0])

        BaseTemplate.applyTemplate(
            project,
            mainClassFile,
            MinecraftFileTemplateGroupFactory.PLACEHOLDERAPI_MAIN_CLASS_TEMPLATE,
            properties
        )
    }

    fun applyPomTemplate(project: Project): String {
        val properties = Properties()

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.PLACEHOLDERAPI_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }
}
