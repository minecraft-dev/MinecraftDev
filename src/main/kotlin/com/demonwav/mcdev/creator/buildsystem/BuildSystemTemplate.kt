/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MULTI_MODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MULTI_MODULE_COMMON_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MULTI_MODULE_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MULTI_MODULE_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.MULTI_MODULE_SETTINGS_GRADLE_TEMPLATE
import com.intellij.openapi.project.Project

object BuildSystemTemplate : BaseTemplate() {

    fun applyPom(project: Project): String {
        return project.applyTemplate(MULTI_MODULE_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    }

    fun applyCommonPom(project: Project): String {
        return project.applyTemplate(MULTI_MODULE_COMMON_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_VERSION" to buildSystem.version
        )

        return project.applyTemplate(MULTI_MODULE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String {
        return project.applyTemplate(MULTI_MODULE_GRADLE_PROPERTIES_TEMPLATE)
    }

    fun applySettingsGradle(project: Project, buildSystem: BuildSystem, subProjects: List<String>): String {
        val props = mapOf(
            "ARTIFACT_ID" to buildSystem.artifactId,
            "INCLUDES" to subProjects.joinToString(", ") { "'$it'" }
        )

        return project.applyTemplate(MULTI_MODULE_SETTINGS_GRADLE_TEMPLATE, props)
    }
}
