/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_PLUGINS_JSON_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE8_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.intellij.openapi.project.Project

object Sponge8Template : BaseTemplate() {

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

    fun applyPluginsJson(
        project: Project,
        buildSystem: BuildSystem,
        config: SpongeProjectConfig
    ): String {
        val props = mutableMapOf(
            "PLUGIN_ID" to buildSystem.artifactId,
            "VERSION_PLACEHOLDER" to "\${version}",
            "SPONGEAPI_VERSION" to config.spongeApiVersion,
            "LICENSE" to config.license,
            "PLUGIN_NAME" to config.pluginName,
            "MAIN_CLASS" to config.mainClass,
            "DESCRIPTION" to config.description,
            "WEBSITE" to config.website,
            "AUTHORS" to config.authors,
            "DEPENDENCIES" to config.dependencies
        )

        return project.applyTemplate(SPONGE8_PLUGINS_JSON_TEMPLATE, props)
    }

    fun applyBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: SpongeProjectConfig
    ): String {
        val props = mutableMapOf(
            "JAVA_VERSION" to config.javaVersion.feature,
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_ID" to buildSystem.artifactId,
            "PLUGIN_VERSION" to buildSystem.version,
            "SPONGEAPI_VERSION" to config.spongeApiVersion,
            "LICENSE" to config.license,
            "PLUGIN_NAME" to config.pluginName,
            "MAIN_CLASS" to config.mainClass,
            "DESCRIPTION" to config.description,
            "WEBSITE" to config.website,
            "AUTHORS" to config.authors,
            "DEPENDENCIES" to config.dependencies
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

    fun applySubBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: SpongeProjectConfig
    ): String {
        val props = mutableMapOf(
            "JAVA_VERSION" to config.javaVersion.feature,
            "PLUGIN_ID" to buildSystem.parentOrError.artifactId,
            "SPONGEAPI_VERSION" to config.spongeApiVersion,
            "LICENSE" to config.license,
            "PLUGIN_NAME" to config.pluginName,
            "MAIN_CLASS" to config.mainClass,
            "DESCRIPTION" to config.description,
            "WEBSITE" to config.website,
            "AUTHORS" to config.authors,
            "DEPENDENCIES" to config.dependencies,
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName
        )

        return project.applyTemplate(SPONGE8_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }
}
