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
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.SPONGE_SUBMODULE_POM_TEMPLATE
import com.intellij.openapi.project.Project

object SpongeTemplate : BaseTemplate() {

    fun applyPom(project: Project, config: SpongeProjectConfig): String {
        val props = BasicMavenStep.pluginVersions.toMutableMap()
        props["JAVA_VERSION"] = config.javaVersion.toString()

        return project.applyTemplate(SPONGE_POM_TEMPLATE, props)
    }

    fun applySubPom(project: Project, config: SpongeProjectConfig): String {
        val props = BasicMavenStep.pluginVersions.toMutableMap()
        props["JAVA_VERSION"] = config.javaVersion.toString()

        return project.applyTemplate(SPONGE_SUBMODULE_POM_TEMPLATE, props)
    }

    fun applyMainClass(
        project: Project,
        packageName: String,
        className: String,
        hasDependencies: Boolean
    ): String {
        val props = mutableMapOf(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className
        )

        if (hasDependencies) {
            props["HAS_DEPENDENCIES"] = "true"
        }

        return project.applyTemplate(SPONGE_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_ID" to buildSystem.artifactId,
            "PLUGIN_VERSION" to buildSystem.version
        )

        return project.applyTemplate(SPONGE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String {
        return project.applyTemplate(SPONGE_GRADLE_PROPERTIES_TEMPLATE)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(SPONGE_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName,
            "PLUGIN_ID" to buildSystem.artifactId
        )

        return project.applyTemplate(SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }
}
