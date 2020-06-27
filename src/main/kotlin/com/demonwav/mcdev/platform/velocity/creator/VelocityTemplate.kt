/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_SUBMODULE_POM_TEMPLATE
import com.intellij.openapi.project.Project

object VelocityTemplate : BaseTemplate() {

    fun applyPom(project: Project): String =
        project.applyTemplate(VELOCITY_POM_TEMPLATE, BasicMavenStep.pluginVersions)

    fun applySubPom(project: Project): String =
        project.applyTemplate(VELOCITY_SUBMODULE_POM_TEMPLATE, BasicMavenStep.pluginVersions)

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

        return project.applyTemplate(VELOCITY_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_ID" to buildSystem.artifactId,
            "PLUGIN_VERSION" to buildSystem.version
        )

        return project.applyTemplate(VELOCITY_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String = project.applyTemplate(VELOCITY_GRADLE_PROPERTIES_TEMPLATE)

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf("ARTIFACT_ID" to artifactId)

        return project.applyTemplate(VELOCITY_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName,
            "PLUGIN_ID" to buildSystem.artifactId
        )

        return project.applyTemplate(VELOCITY_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }
}
