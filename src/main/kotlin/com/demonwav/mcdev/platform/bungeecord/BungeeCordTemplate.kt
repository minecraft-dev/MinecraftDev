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
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.bukkit.BukkitTemplate
import com.demonwav.mcdev.util.MinecraftFileTemplateGroupFactory
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.Properties

object BungeeCordTemplate {

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
            MinecraftFileTemplateGroupFactory.BUNGEECORD_MAIN_CLASS_TEMPLATE,
            properties
        )
    }

    fun applyPomTemplate(project: Project): String {
        val properties = Properties()

        val manager = FileTemplateManager.getInstance(project)
        val fileTemplate = manager.getJ2eeTemplate(MinecraftFileTemplateGroupFactory.BUNGEECORD_POM_TEMPLATE)
        return fileTemplate.getText(properties)
    }

    fun applyPluginDescriptionFileTemplate(
        project: Project,
        file: VirtualFile,
        config: BungeeCordProjectConfiguration,
        buildSystem: BuildSystem
    ) {
        val base = config.base ?: return

        val properties = BukkitTemplate.bukkitMain(buildSystem, base)
        BukkitTemplate.bukkitDeps(properties, config)

        if (config.hasAuthors()) {
            // BungeeCord only supports one author
            properties.setProperty("AUTHOR", base.authors[0])
            properties.setProperty("HAS_AUTHOR", "true")
        }

        if (config.hasDescription()) {
            properties.setProperty("DESCRIPTION", base.description)
            properties.setProperty("HAS_DESCRIPTION", "true")
        }

        BaseTemplate.applyTemplate(
            project,
            file,
            MinecraftFileTemplateGroupFactory.BUKKIT_PLUGIN_YML_TEMPLATE,
            properties,
            true
        )
    }
}
