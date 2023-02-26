/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.creator

import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.step.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.IssueTrackerStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.architectury.ArchitecturyVersion
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project

class ArchitecturyVersionData(
    val forgeVersions: ForgeVersion,
    val fabricVersions: FabricVersions,
    val fabricApiVersions: FabricApiVersions,
    val architecturyVersions: ArchitecturyVersion,
)

private val NewProjectWizardStep.architecturyPackage: String get() {
    val apiVersion = data.getUserData(ArchitecturyVersionChainStep.ARCHITECTURY_API_VERSION_KEY)
    return when {
        apiVersion == null || apiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
        else -> "me.shedaniel.architectury"
    }
}

class ArchitecturyProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Adding Architectury project files (phase 1)"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        val javaVersion = findStep<JdkProjectSetupFinalizer>().preferredJdk.ordinal
        val packageName = "${buildSystemProps.groupId.toPackageName()}.${buildSystemProps.artifactId.toPackageName()}"
        val mcVersion = data.getUserData(ArchitecturyVersionChainStep.MC_VERSION_KEY) ?: return
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val forgeVersion = data.getUserData(ArchitecturyVersionChainStep.FORGE_VERSION_KEY) ?: return
        val fabricLoaderVersion = data.getUserData(ArchitecturyVersionChainStep.FABRIC_LOADER_VERSION_KEY) ?: return
        val fabricApiVersion = data.getUserData(ArchitecturyVersionChainStep.FABRIC_API_VERSION_KEY)
        val archApiVersion = data.getUserData(ArchitecturyVersionChainStep.ARCHITECTURY_API_VERSION_KEY)
        val license = data.getUserData(LicenseStep.KEY) ?: return
        val authors = data.getUserData(AuthorsStep.KEY) ?: emptyList()
        val website = data.getUserData(WebsiteStep.KEY) ?: ""
        val issueTracker = data.getUserData(IssueTrackerStep.KEY) ?: ""
        val description = data.getUserData(DescriptionStep.KEY) ?: ""

        val hasDisplayTestInManifest = forgeVersion >= ForgeConstants.DISPLAY_TEST_MANIFEST_VERSION
        val nextMcVersion = when (val part = mcVersion.parts.getOrNull(1)) {
            // Mimics the code used to get the next Minecraft version in Forge's MDK
            // https://github.com/MinecraftForge/MinecraftForge/blob/0ff8a596fc1ef33d4070be89dd5cb4851f93f731/build.gradle#L884
            is SemanticVersion.Companion.VersionPart.ReleasePart -> (part.version + 1).toString()
            null -> "?"
            else -> part.versionString
        }

        assets.addAssets(
            GeneratorEmptyDirectory("common/src/main/java"),
            GeneratorEmptyDirectory("common/src/main/resources"),
            GeneratorEmptyDirectory("forge/src/main/java"),
            GeneratorEmptyDirectory("forge/src/main/resources"),
            GeneratorEmptyDirectory("fabric/src/main/java"),
            GeneratorEmptyDirectory("fabric/src/main/resources"),
        )

        val packDescriptor = ForgePackDescriptor.forMcVersion(mcVersion) ?: ForgePackDescriptor.FORMAT_3
        val packAdditionalData = ForgePackAdditionalData.forMcVersion(mcVersion)

        assets.addTemplateProperties(
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "PACK_FORMAT" to packDescriptor.format,
            "PACK_COMMENT" to packDescriptor.comment,
            "PACKAGE_NAME" to packageName,
            "JAVA_VERSION" to javaVersion,
            "MOD_NAME" to modName,
            "DISPLAY_TEST" to hasDisplayTestInManifest,
            "FORGE_SPEC_VERSION" to forgeVersion.parts[0].versionString,
            "MC_VERSION" to mcVersion,
            "MC_NEXT_VERSION" to "1.$nextMcVersion",
            "LICENSE" to license,
            "DESCRIPTION" to description,
            "MOD_DESCRIPTION" to description,
            "MOD_ENVIRONMENT" to "*",
            "FABRIC_LOADER_VERSION" to fabricLoaderVersion,
        )

        if (fabricApiVersion != null) {
            assets.addTemplateProperties(
                "FABRIC_API_VERSION" to fabricApiVersion,
            )
        }

        if (archApiVersion != null) {
            assets.addTemplateProperties(
                "ARCHITECTURY_API_VERSION" to archApiVersion,
            )
        }

        if (authors.isNotEmpty()) {
            assets.addTemplateProperties("AUTHOR_LIST" to authors.joinToString(", "))
        }

        if (website.isNotBlank()) {
            assets.addTemplateProperties("WEBSITE" to website)
        }

        if (issueTracker.isNotBlank()) {
            assets.addTemplateProperties("ISSUE" to issueTracker)
        }

        if (packAdditionalData != null) {
            assets.addTemplateProperties(
                "FORGE_DATA" to packAdditionalData,
            )
        }

        if (useMixins) {
            assets.addTemplateProperties(
                "MIXINS" to "true"
            )
            val commonMixinsFile = "common/src/main/resources/${buildSystemProps.artifactId}-common.mixins.json"
            val forgeMixinsFile = "forge/src/main/resources/${buildSystemProps.artifactId}.mixins.json"
            val fabricMixinsFile = "fabric/src/main/resources/${buildSystemProps.artifactId}.mixins.json"
            assets.addTemplates(
                project,
                commonMixinsFile to MinecraftTemplates.ARCHITECTURY_COMMON_MIXINS_JSON_TEMPLATE,
                forgeMixinsFile to MinecraftTemplates.ARCHITECTURY_FORGE_MIXINS_JSON_TEMPLATE,
                fabricMixinsFile to MinecraftTemplates.ARCHITECTURY_FABRIC_MIXINS_JSON_TEMPLATE,
            )
        }

        assets.addTemplates(
            project,
            "forge/src/main/resources/pack.mcmeta" to MinecraftTemplates.ARCHITECTURY_FORGE_PACK_MCMETA_TEMPLATE,
            "forge/src/main/resources/META-INF/mods.toml" to MinecraftTemplates.ARCHITECTURY_FORGE_MODS_TOML_TEMPLATE,
            "fabric/src/main/resources/fabric.mod.json" to MinecraftTemplates.ARCHITECTURY_FABRIC_MOD_JSON_TEMPLATE,
        )

        assets.addLicense(project)
    }
}

abstract class ArchitecturyMainClassStep(
    parent: NewProjectWizardStep,
    phase: Int
) : AbstractLongRunningAssetsStep(parent) {
    abstract val projectDir: String
    abstract val template: String
    abstract fun getClassName(packageName: String, className: String): String

    override val description = "Adding Architectury project files (phase $phase)"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val useArchApi = data.getUserData(ArchitecturyVersionChainStep.ARCHITECTURY_API_VERSION_KEY) != null

        val packageName = "${buildSystemProps.groupId.toPackageName()}.${buildSystemProps.artifactId.toPackageName()}"
        val className = modName.toJavaClassName()
        assets.addTemplateProperties(
            "PACKAGE_NAME" to packageName,
            "CLASS_NAME" to className,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_NAME" to modName,
            "MOD_VERSION" to buildSystemProps.version,
            "ARCHITECTURY_PACKAGE" to architecturyPackage,
        )
        if (useArchApi) {
            assets.addTemplateProperties("ARCHITECTURY_API" to "true")
        }

        val mainClass = getClassName(packageName, className)
        assets.addTemplates(project, "$projectDir/src/main/java/${mainClass.replace('.', '/')}.java" to template)
    }
}

class ArchitecturyCommonMainClassStep(parent: NewProjectWizardStep) : ArchitecturyMainClassStep(parent, 2) {
    override val projectDir = "common"
    override val template = MinecraftTemplates.ARCHITECTURY_COMMON_MAIN_CLASS_TEMPLATE

    override fun getClassName(packageName: String, className: String) = "$packageName.$className"
}

class ArchitecturyForgeMainClassStep(parent: NewProjectWizardStep) : ArchitecturyMainClassStep(parent, 3) {
    override val projectDir = "forge"
    override val template = MinecraftTemplates.ARCHITECTURY_FORGE_MAIN_CLASS_TEMPLATE

    override fun getClassName(packageName: String, className: String) = "$packageName.forge.${className}Forge"
}

class ArchitecturyFabricMainClassStep(parent: NewProjectWizardStep) : ArchitecturyMainClassStep(parent, 4) {
    override val projectDir = "fabric"
    override val template = MinecraftTemplates.ARCHITECTURY_FABRIC_MAIN_CLASS_TEMPLATE

    override fun getClassName(packageName: String, className: String) = "$packageName.fabric.${className}Fabric"
}

class ArchitecturyBuildSystemStep(parent: NewProjectWizardStep) : AbstractBuildSystemStep(parent) {
    override val platformName = "Architectury"
}

class ArchitecturyPostBuildSystemStep(
    parent: NewProjectWizardStep
) : AbstractRunBuildSystemStep(parent, ArchitecturyBuildSystemStep::class.java) {
    override val step = BuildSystemSupport.POST_STEP
}
