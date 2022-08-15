/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.creator

import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.BaseTemplate
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.util.License
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.toPackageName
import com.intellij.openapi.project.Project

object ArchitecturyTemplate : BaseTemplate() {
    private fun Project.applyGradleTemplate(
        templateName: String,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        val props = mutableMapOf<String, Any>(
            "GROUP_ID" to buildSystem.groupId,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "VERSION" to buildSystem.version,
            "MC_VERSION" to config.mcVersion.toString(),
            "FORGE_VERSION" to config.forgeVersionText,
            "FABRIC_LOADER_VERSION" to config.fabricLoaderVersion.toString(),
            "FABRIC_API_VERSION" to config.fabricApiVersion.toString(),
            "ARCHITECTURY_API_VERSION" to config.architecturyApiVersion.toString(),
            "ARCHITECTURY_GROUP" to config.architecturyGroup,
            "LOOM_VERSION" to config.loomVersion.toString(),
            "JAVA_VERSION" to config.javaVersion.feature
        )
        if (config.fabricApi) {
            props["FABRIC_API"] = "true"
        }
        if (config.architecturyApi) {
            props["ARCHITECTURY_API"] = "true"
        }
        return applyTemplate(templateName, props)
    }

    fun applyBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(MinecraftTemplates.ARCHITECTURY_BUILD_GRADLE_TEMPLATE, buildSystem, config)
    }

    fun applyGradleProp(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_GRADLE_PROPERTIES_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyMultiModuleBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_SUBMODULE_BUILD_GRADLE_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyMultiModuleGradleProp(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_SUBMODULE_GRADLE_PROPERTIES_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applySettingsGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_SETTINGS_GRADLE_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyCommonBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_COMMON_BUILD_GRADLE_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyForgeBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_FORGE_BUILD_GRADLE_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyForgeGradleProp(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_FORGE_GRADLE_PROPERTIES_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyFabricBuildGradle(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        return project.applyGradleTemplate(
            MinecraftTemplates.ARCHITECTURY_FABRIC_BUILD_GRADLE_TEMPLATE,
            buildSystem,
            config
        )
    }

    fun applyFabricModJsonTemplate(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        val props = mutableMapOf<String, Any>(
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "MOD_DESCRIPTION" to (config.description ?: ""),
            "MOD_ENVIRONMENT" to "*",
            "FABRIC_LOADER_VERSION" to config.fabricLoaderVersion.toString(),
            "FABRIC_API_VERSION" to config.fabricApiVersion.toString(),
            "ARCHITECTURY_API_VERSION" to config.architecturyApiVersion.toString(),
            "MC_VERSION" to config.mcVersion.toString(),
            "JAVA_VERSION" to config.javaVersion.feature,
            "LICENSE" to ((config.license ?: License.ALL_RIGHTS_RESERVED).id)
        )
        if (config.mixins) {
            props["MIXINS"] = "true"
        }

        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FABRIC_MOD_JSON_TEMPLATE, props)
    }

    fun applyModsToml(project: Project, buildSystem: BuildSystem, config: ArchitecturyProjectConfig): String {
        val hasDisplayTestInManifest = config.forgeVersion >= ForgeConstants.DISPLAY_TEST_MANIFEST_VERSION
        val nextMcVersion = when (val part = config.mcVersion.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in Forge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }
        val props = mutableMapOf(
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "DISPLAY_TEST" to hasDisplayTestInManifest,
            "FORGE_SPEC_VERSION" to config.forgeVersion.parts[0].versionString,
            "ARCHITECTURY_API_VERSION" to config.architecturyApiVersion.toString(),
            "MC_VERSION" to config.mcVersion.toString(),
            "MC_NEXT_VERSION" to "1.$nextMcVersion",
            "LICENSE" to config.license.toString()
        )
        props["DESCRIPTION"] = config.description ?: ""
        if (config.hasAuthors()) {
            props["AUTHOR_LIST"] = config.authors.joinToString(", ")
        }
        if (config.hasWebsite()) {
            props["WEBSITE"] = config.website.toString()
        }
        if (config.hasIssue()) {
            props["ISSUE"] = config.modIssue.toString()
        }

        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FORGE_MODS_TOML_TEMPLATE, props)
    }

    fun applyPackMcmeta(
        project: Project,
        artifactId: String,
        pack: ForgePackDescriptor,
        additionalData: ForgePackAdditionalData?
    ): String {
        val props = mapOf(
            "ARTIFACT_ID" to artifactId,
            "PACK_FORMAT" to pack.format.toString(),
            "PACK_COMMENT" to pack.comment,
            "FORGE_DATA" to additionalData,
        )

        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FORGE_PACK_MCMETA_TEMPLATE, props)
    }

    fun applyCommonMixinConfigTemplate(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        val packageName = buildSystem.groupId.toPackageName() + "." + buildSystem.artifactId
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "JAVA_VERSION" to config.javaVersion.feature
        )
        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_COMMON_MIXINS_JSON_TEMPLATE, props)
    }

    fun applyForgeMixinConfigTemplate(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        val packageName = buildSystem.groupId.toPackageName() + "." + buildSystem.artifactId
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "JAVA_VERSION" to config.javaVersion.feature
        )
        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FORGE_MIXINS_JSON_TEMPLATE, props)
    }

    fun applyFabricMixinConfigTemplate(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig
    ): String {
        val packageName = buildSystem.groupId.toPackageName() + "." + buildSystem.artifactId
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "JAVA_VERSION" to config.javaVersion.feature
        )
        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FABRIC_MIXINS_JSON_TEMPLATE, props)
    }

    fun applyCommonMainClass(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig,
        packageName: String,
        className: String
    ): String {
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "CLASS_NAME" to className,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "MOD_VERSION" to buildSystem.version
        )

        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_COMMON_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyForgeMainClass(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig,
        packageName: String,
        className: String
    ): String {
        val props = mutableMapOf(
            "PACKAGE_NAME" to packageName,
            "CLASS_NAME" to className,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "MOD_VERSION" to buildSystem.version,
            "ARCHITECTURY_PACKAGE" to config.architecturyPackage
        )

        if (config.architecturyApi) {
            props["ARCHITECTURY_API"] = "true"
        }

        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FORGE_MAIN_CLASS_TEMPLATE, props)
    }

    fun applyFabricMainClass(
        project: Project,
        buildSystem: BuildSystem,
        config: ArchitecturyProjectConfig,
        packageName: String,
        className: String
    ): String {
        val props = mapOf(
            "PACKAGE_NAME" to packageName,
            "CLASS_NAME" to className,
            "ARTIFACT_ID" to buildSystem.artifactId,
            "MOD_NAME" to config.pluginName,
            "MOD_VERSION" to buildSystem.version
        )

        return project.applyTemplate(MinecraftTemplates.ARCHITECTURY_FABRIC_MAIN_CLASS_TEMPLATE, props)
    }
}
