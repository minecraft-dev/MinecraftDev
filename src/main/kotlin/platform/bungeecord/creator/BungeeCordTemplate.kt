/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bungeecord.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.bukkit.creator.BukkitTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUKKIT_PLUGIN_YML_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.BUNGEECORD_SUBMODULE_POM_TEMPLATE
import com.intellij.openapi.project.Project

object BungeeCordTemplate : BaseTemplate() {

    fun applyMainClass(
        project: Project,
        packageName: String,
        className: String
    ): String {
        val props = mapOf(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className
        )

        return project.applyTemplate(BUNGEECORD_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyPom(project: Project): String {
        return project.applyTemplate(BUNGEECORD_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    }

    fun applySubPom(project: Project): String {
        return project.applyTemplate(BUNGEECORD_SUBMODULE_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_VERSION" to buildSystem.version
        )

        return project.applyTemplate(BUNGEECORD_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String {
        return project.applyTemplate(BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(BUNGEECORD_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName
        )

        return project.applyTemplate(BUNGEECORD_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyBungeeYml(
        project: Project,
        config: BungeeCordProjectConfig,
        buildSystem: BuildSystem
    ): String {
        val props = BukkitTemplate.bukkitMain(buildSystem.type, config)

        if (config.hasAuthors()) {
            // BungeeCord only supports one author
            props["AUTHOR"] = config.authors[0]
        }

        if (config.hasDescription()) {
            props["DESCRIPTION"] = config.description
                ?: throw IllegalStateException("description is null when not blank")
        }

        return project.applyTemplate(BUKKIT_PLUGIN_YML_TEMPLATE, props)
    }
}
