/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.velocity.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.velocity.util.VelocityConstants
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_BUILD_CONSTANTS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_MAIN_CLASS_V2_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.VELOCITY_SUBMODULE_POM_TEMPLATE
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.project.Project

object VelocityTemplate : BaseTemplate() {

    fun applyPom(project: Project, config: VelocityProjectConfig): String {
        val props = BasicMavenStep.pluginVersions + ("JAVA_VERSION" to config.javaVersion.toFeatureString())
        return project.applyTemplate(VELOCITY_POM_TEMPLATE, props)
    }

    fun applySubPom(project: Project, config: VelocityProjectConfig): String {
        val props = BasicMavenStep.pluginVersions + ("JAVA_VERSION" to config.javaVersion.toFeatureString())
        return project.applyTemplate(VELOCITY_SUBMODULE_POM_TEMPLATE, props)
    }

    fun applyMainClass(
        project: Project,
        packageName: String,
        className: String,
        hasDependencies: Boolean,
        version: SemanticVersion
    ): String {
        val props = mutableMapOf(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className
        )

        if (hasDependencies) {
            props["HAS_DEPENDENCIES"] = "true"
        }

        val template = if (version < VelocityConstants.API_2 ||
            (version >= VelocityConstants.API_3 && version < VelocityConstants.API_4)
        ) {
            VELOCITY_MAIN_CLASS_TEMPLATE // API 1 and 3
        } else {
            VELOCITY_MAIN_CLASS_V2_TEMPLATE // API 2 and 4 (4+ maybe ?)
        }

        return project.applyTemplate(template, props)
    }

    fun applyBuildConstants(project: Project, packageName: String): String {
        val props = mapOf(
            "PACKAGE" to packageName
        )

        return project.applyTemplate(VELOCITY_BUILD_CONSTANTS_TEMPLATE, props)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem, config: VelocityProjectConfig): String {
        val javaVersion = config.javaVersion.feature
        val props = mapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_ID" to buildSystem.artifactId,
            "PLUGIN_VERSION" to buildSystem.version,
            "JAVA_VERSION" to javaVersion
        )

        return project.applyTemplate(VELOCITY_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project, javaVersion: Int?): String =
        project.applyTemplate(VELOCITY_GRADLE_PROPERTIES_TEMPLATE, mapOf("JAVA_VERSION" to javaVersion))

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
