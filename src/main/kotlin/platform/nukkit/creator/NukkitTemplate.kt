/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.nukkit.creator

import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.buildsystem.maven.BasicMavenStep
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.nukkit.NukkitLikeConfiguration
import com.demonwav.mcdev.platform.nukkit.data.LoadOrder
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_GRADLE_PROPERTIES_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_MAIN_CLASS_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_PLUGIN_YML_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_POM_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_SETTINGS_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_SUBMODULE_BUILD_GRADLE_TEMPLATE
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.NUKKIT_SUBMODULE_POM_TEMPLATE
import com.intellij.openapi.project.Project

object NukkitTemplate : BaseTemplate() {

    fun applyMainClass(
        project: Project,
        packageName: String,
        className: String
    ): String {
        val props = mapOf(
            "PACKAGE" to packageName,
            "CLASS_NAME" to className
        )

        return project.applyTemplate(NUKKIT_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyPom(project: Project): String {
        return project.applyTemplate(NUKKIT_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    }

    fun applySubPom(project: Project): String {
        return project.applyTemplate(NUKKIT_SUBMODULE_POM_TEMPLATE, BasicMavenStep.pluginVersions)
    }

    fun applyBuildGradle(project: Project, buildSystem: BuildSystem, config: NukkitProjectConfig): String {
        val props = mapOf(
            "GROUP_ID" to buildSystem.groupId,
            "PLUGIN_VERSION" to buildSystem.version,
            "JAVA_VERSION" to config.javaVersion.feature
        )

        return project.applyTemplate(NUKKIT_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyGradleProp(project: Project): String {
        return project.applyTemplate(NUKKIT_GRADLE_PROPERTIES_TEMPLATE)
    }

    fun applySettingsGradle(project: Project, artifactId: String): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId
        )

        return project.applyTemplate(NUKKIT_SETTINGS_GRADLE_TEMPLATE, props)
    }

    fun applySubBuildGradle(project: Project, buildSystem: BuildSystem): String {
        val props = mapOf(
            "COMMON_PROJECT_NAME" to buildSystem.commonModuleName
        )

        return project.applyTemplate(NUKKIT_SUBMODULE_BUILD_GRADLE_TEMPLATE, props)
    }

    fun applyPluginYml(
        project: Project,
        config: NukkitProjectConfig,
        buildSystem: BuildSystem
    ): String {
        fun nukkitDeps(props: MutableMap<String, String>, configuration: NukkitLikeConfiguration) {
            if (configuration.hasDependencies()) {
                props["DEPEND"] = configuration.dependencies.toString()
            }
            if (configuration.hasSoftDependencies()) {
                props["SOFT_DEPEND"] = configuration.softDependencies.toString()
            }
        }

        val props = nukkitMain(buildSystem.type, config)

        if (config.hasPrefix()) {
            props["PREFIX"] = config.prefix ?: throw IllegalStateException("prefix is null when not blank")
        }

        if (config.loadOrder != LoadOrder.POSTWORLD) {
            props["LOAD"] = LoadOrder.STARTUP.name
        }

        if (config.hasLoadBefore()) {
            props["LOAD_BEFORE"] = config.loadBefore.toString()
        }

        nukkitDeps(props, config)

        if (config.hasAuthors()) {
            props["AUTHOR_LIST"] = config.authors.toString()
        }

        if (config.hasDescription()) {
            props["DESCRIPTION"] = config.description
                ?: throw IllegalStateException("description is null when not blank")
        }

        if (config.hasWebsite()) {
            props["WEBSITE"] = config.website ?: throw IllegalStateException("website is null when not blank")
        }

        return project.applyTemplate(NUKKIT_PLUGIN_YML_TEMPLATE, props)
    }

    private fun <C> nukkitMain(type: BuildSystemType, config: C): MutableMap<String, String>
        where C : ProjectConfig,
              C : NukkitLikeConfiguration {
        val version = when (type) {
            BuildSystemType.GRADLE -> "\${version}"
            BuildSystemType.MAVEN -> "\${project.version}"
        }

        return mutableMapOf(
            "MAIN" to config.mainClass,
            "VERSION" to version,
            "NAME" to config.pluginName
        )
    }
}
