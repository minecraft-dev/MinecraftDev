/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_SETTINGS_GRADLE_TEMPLATE
import com.intellij.openapi.project.Project

object Sponge8Template : BaseTemplate() {

    // fun applyPom(project: Project): String {
    //     return project.applyTemplate(SPONGE8_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    // }
    //
    // fun applySubPom(project: Project): String {
    //     return project.applyTemplate(SPONGE8_SUBMODULE_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    // }

    fun applyMainClass(
        project: Project,
        pluginId: String,
        packageName: String,
        className: String
    ): String {
        val props = mutableMapOf(
            "PLUGIN_ID" to pluginId,
            "PACKAGE" to packageName,
            "CLASS_NAME" to className
        )

        return project.applyTemplate(SPONGE8_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        spongeApiVersion: String,
        pluginName: String,
        mainClass: String,
        description: String?,
        website: String?,
        authors: List<String>,
        dependencies: List<String>
    ): String {
        val props = mutableMapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_ID" to buildSystem.artifactId,
            "PLUGIN_VERSION" to buildSystem.version,
            "SPONGEAPI_VERSION" to spongeApiVersion.removeSuffix("-SNAPSHOT"), // SpongeGradle 1.1.0 adds the -SNAPSHOT suffix itself
            "PLUGIN_NAME" to pluginName,
            "MAIN_CLASS" to mainClass,
            "DESCRIPTION" to description,
            "WEBSITE" to website,
            "AUTHORS" to authors,
            "DEPENDENCIES" to dependencies
        )

        return project.applyTemplate(SPONGE8_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String {
        return project.applyTemplate(SPONGE8_GRADLE_PROPERTIES_TEMPLATE)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(SPONGE8_SETTINGS_GRADLE_TEMPLATE, props)
    }

    // fun applySubBuildGradle(project: Project, buildSystem: BuildSystem): String {
    //     val props = mapOf(
    //         "COMMON_PROJECT_NAME" to buildSystem.commonModuleName,
    //         "PLUGIN_ID" to buildSystem.artifactId
    //     )
    //
    //     return project.applyTemplate(SPONGE8_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    // }
}
