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

import com.demonwav.mcdev.creator.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.AbstractLatentStep
import com.demonwav.mcdev.creator.AbstractLongRunningAssetsStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractSelectVersionStep
import com.demonwav.mcdev.creator.AbstractSelectVersionThenForkStep
import com.demonwav.mcdev.creator.AuthorsStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.EmptyStep
import com.demonwav.mcdev.creator.IssueTrackerStep
import com.demonwav.mcdev.creator.JdkProjectSetupFinalizer
import com.demonwav.mcdev.creator.LicenseStep
import com.demonwav.mcdev.creator.ModNameStep
import com.demonwav.mcdev.creator.RepositoryStep
import com.demonwav.mcdev.creator.UseMixinsStep
import com.demonwav.mcdev.creator.WebsiteStep
import com.demonwav.mcdev.creator.addLicense
import com.demonwav.mcdev.creator.addTemplates
import com.demonwav.mcdev.creator.buildsystem.AbstractBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.AbstractRunBuildSystemStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemSupport
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleImportStep
import com.demonwav.mcdev.creator.buildsystem.gradle.GradleWrapperStep
import com.demonwav.mcdev.creator.buildsystem.gradle.addGradleWrapperProperties
import com.demonwav.mcdev.creator.chain
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.platform.architectury.version.ArchitecturyVersion
import com.demonwav.mcdev.platform.fabric.creator.FabricApiVersionStep
import com.demonwav.mcdev.platform.fabric.creator.FabricLoaderVersionStep
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.creator.ForgeVersionStep
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.forge.util.ForgePackAdditionalData
import com.demonwav.mcdev.platform.forge.util.ForgePackDescriptor
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.MinecraftTemplates
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.toJavaClassName
import com.demonwav.mcdev.util.toPackageName
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.chain
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import kotlinx.coroutines.coroutineScope

class ArchitecturyVersionData(
    val forgeVersions: ForgeVersion,
    val fabricVersions: FabricVersions,
    val fabricApiVersions: FabricApiVersions,
    val architecturyVersions: ArchitecturyVersion,
)

private val NewProjectWizardStep.architecturyGroup: String get() {
    val apiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
    return when {
        apiVersion == null || apiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
        else -> "me.shedaniel"
    }
}

private val NewProjectWizardStep.architecturyPackage: String get() {
    val apiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
    return when {
        apiVersion == null || apiVersion >= SemanticVersion.release(2, 0, 10) -> "dev.architectury"
        else -> "me.shedaniel.architectury"
    }
}

class ArchitecturyPlatformStep(parent: ModPlatformStep) : AbstractLatentStep<ArchitecturyVersionData>(parent) {
    override val description = "download Forge, Fabric and Architectury versions"

    override suspend fun computeData() = coroutineScope {
        val forgeJob = asyncIO { ForgeVersion.downloadData() }
        val fabricJob = asyncIO { FabricVersions.downloadData() }
        val fabricApiJob = asyncIO { FabricApiVersions.downloadData() }
        val archJob = asyncIO { ArchitecturyVersion.downloadData() }

        val forge = forgeJob.await() ?: return@coroutineScope null
        val fabric = fabricJob.await() ?: return@coroutineScope null
        val fabricApi = fabricApiJob.await() ?: return@coroutineScope null
        val arch = archJob.await() ?: return@coroutineScope null

        ArchitecturyVersionData(forge, fabric, fabricApi, arch)
    }

    override fun createStep(data: ArchitecturyVersionData): NewProjectWizardStep {
        val mcVersions = data.architecturyVersions.versions.keys.intersect(data.forgeVersions.sortedMcVersions.toSet())
            .intersect(data.fabricVersions.game.mapNotNullTo(mutableSetOf()) { SemanticVersion.tryParse(it.version) })
        return ArchitecturyMcVersionStep(this, mcVersions.toList(), data).chain(
            ::UseMixinsStep,
            ::ModNameStep,
            ::LicenseStep,
            ::ArchitecturyOptionalSettingsStep,
            ::ArchitecturyBuildSystemStep,
            ::ArchitecturyProjectFilesStep,
            ::ArchitecturyCommonMainClassStep,
            ::ArchitecturyForgeMainClassStep,
            ::ArchitecturyFabricMainClassStep,
            ::ArchitecturyPostBuildSystemStep,
        )
    }

    class Factory : ModPlatformStep.Factory {
        override val name = "Architectury"
        override fun createStep(parent: ModPlatformStep) = ArchitecturyPlatformStep(parent)
    }
}

class ArchitecturyMcVersionStep(
    parent: NewProjectWizardStep,
    mcVersions: List<SemanticVersion>,
    private val versionData: ArchitecturyVersionData
) : AbstractSelectVersionThenForkStep<SemanticVersion>(parent, mcVersions) {
    override val label = "Minecraft Version:"

    override fun initStep(version: SemanticVersion) =
        ForgeVersionStep(this, versionData.forgeVersions.getForgeVersions(version)).chain(
            { parent -> FabricLoaderVersionStep(parent, versionData.fabricVersions.loader) },
            { parent ->
                val versionStr = version.toString()
                val apiVersions = versionData.fabricApiVersions.versions
                    .filter { versionStr in it.gameVersions }
                    .map { it.version }
                if (apiVersions.isEmpty()) {
                    FabricApiVersionStep(parent, versionData.fabricApiVersions.versions.map { it.version }, false)
                } else {
                    FabricApiVersionStep(parent, apiVersions, true)
                }
            },
            { parent ->
                ArchitecturyApiVersionStep(parent, versionData.architecturyVersions.getArchitecturyVersions(version))
            }
        )

    override fun setupProject(project: Project) {
        data.putUserData(KEY, SemanticVersion.tryParse(step))
        super.setupProject(project)
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${ArchitecturyMcVersionStep::class.java.name}.version")
    }
}

class ArchitecturyApiVersionStep(
    parent: NewProjectWizardStep,
    versions: List<SemanticVersion>
) : AbstractSelectVersionStep<SemanticVersion>(parent, versions) {
    override val label = "Architectury API Version:"

    private val useArchitecturyApiProperty = propertyGraph.property(true)
        .bindBooleanStorage("${javaClass.name}.useArchitecturyApi")
    private var useArchitecturyApi by useArchitecturyApiProperty

    override fun setupRow(builder: Row) {
        super.setupRow(builder)

        with(builder) {
            checkBox("Use Architectury API")
                .bindSelected(useArchitecturyApiProperty)
        }

        useArchitecturyApiProperty.afterChange { versionBox.isEnabled = useArchitecturyApi }
        versionBox.isEnabled = useArchitecturyApi
    }

    override fun setupProject(project: Project) {
        if (useArchitecturyApi) {
            data.putUserData(KEY, SemanticVersion.tryParse(version))
        }
    }

    companion object {
        val KEY = Key.create<SemanticVersion>("${ArchitecturyApiVersionStep::class.java.name}.version")
    }
}

class ArchitecturyOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this).chain(
        ::AuthorsStep,
        ::WebsiteStep,
        ::RepositoryStep,
        ::IssueTrackerStep,
    )
}

class ArchitecturyGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Creating Gradle files"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val mcVersion = data.getUserData(ArchitecturyMcVersionStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
        val fabricLoaderVersion = data.getUserData(FabricLoaderVersionStep.KEY) ?: return
        val fabricApiVersion = data.getUserData(FabricApiVersionStep.KEY)
        val archApiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal

        assets.addTemplateProperties(
            "GROUP_ID" to buildSystemProps.groupId,
            "ARTIFACT_ID" to buildSystemProps.artifactId,
            "MOD_NAME" to modName,
            "VERSION" to buildSystemProps.version,
            "MC_VERSION" to mcVersion,
            "FORGE_VERSION" to forgeVersion,
            "FABRIC_LOADER_VERSION" to fabricLoaderVersion,
            "ARCHITECTURY_GROUP" to architecturyGroup,
            "JAVA_VERSION" to javaVersion
        )

        if (fabricApiVersion != null) {
            assets.addTemplateProperties(
                "FABRIC_API_VERSION" to fabricApiVersion,
                "FABRIC_API" to "true",
            )
        }

        if (archApiVersion != null) {
            assets.addTemplateProperties(
                "ARCHITECTURY_API_VERSION" to archApiVersion,
                "ARCHITECTURY_API" to "true",
            )
        }

        assets.addTemplates(
            project,
            "build.gradle" to MinecraftTemplates.ARCHITECTURY_BUILD_GRADLE_TEMPLATE,
            "gradle.properties" to MinecraftTemplates.ARCHITECTURY_GRADLE_PROPERTIES_TEMPLATE,
            "settings.gradle" to MinecraftTemplates.ARCHITECTURY_SETTINGS_GRADLE_TEMPLATE,
            "common/build.gradle" to MinecraftTemplates.ARCHITECTURY_COMMON_BUILD_GRADLE_TEMPLATE,
            "forge/build.gradle" to MinecraftTemplates.ARCHITECTURY_FORGE_BUILD_GRADLE_TEMPLATE,
            "forge/gradle.properties" to MinecraftTemplates.ARCHITECTURY_FORGE_GRADLE_PROPERTIES_TEMPLATE,
            "fabric/build.gradle" to MinecraftTemplates.ARCHITECTURY_FABRIC_BUILD_GRADLE_TEMPLATE,
        )

        assets.addGradleWrapperProperties(project)
    }
}

class ArchitecturyProjectFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningAssetsStep(parent) {
    override val description = "Adding Architectury project files (phase 1)"

    override fun setupAssets(project: Project) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        val useMixins = data.getUserData(UseMixinsStep.KEY) ?: false
        val javaVersion = findStep<JdkProjectSetupFinalizer>().minVersion.ordinal
        val packageName = "${buildSystemProps.groupId.toPackageName()}.${buildSystemProps.artifactId.toPackageName()}"
        val mcVersion = data.getUserData(ArchitecturyMcVersionStep.KEY) ?: return
        val modName = data.getUserData(AbstractModNameStep.KEY) ?: return
        val forgeVersion = data.getUserData(ForgeVersionStep.KEY) ?: return
        val fabricLoaderVersion = data.getUserData(FabricLoaderVersionStep.KEY) ?: return
        val fabricApiVersion = data.getUserData(FabricApiVersionStep.KEY)
        val archApiVersion = data.getUserData(ArchitecturyApiVersionStep.KEY)
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
            val commonMixinsFile = "common/src/main/resources/${buildSystemProps.artifactId}.mixins.json"
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
        val useArchApi = data.getUserData(ArchitecturyApiVersionStep.KEY) != null

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

class ArchitecturyGradleSupport : BuildSystemSupport {
    override val preferred = true

    override fun createStep(step: String, parent: NewProjectWizardStep): NewProjectWizardStep {
        return when (step) {
            BuildSystemSupport.PRE_STEP -> ArchitecturyGradleFilesStep(parent).chain(::GradleWrapperStep)
            BuildSystemSupport.POST_STEP -> GradleImportStep(parent)
            else -> EmptyStep(parent)
        }
    }
}
