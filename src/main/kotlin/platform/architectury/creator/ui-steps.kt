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

import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AbstractMcVersionChainStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.IssueTrackerStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.ModNameStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.RepositoryStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.VersionChainComboBox
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.architectury.ArchitecturyVersion
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.bindEnabled
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.IncorrectOperationException
import kotlinx.coroutines.coroutineScope

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
        return ArchitecturyVersionChainStep(this, data)
            .nextStep(::UseMixinsStep)
            .nextStep(::ModNameStep)
            .nextStep(::LicenseStep)
            .nextStep(::ArchitecturyOptionalSettingsStep)
            .nextStep(::ArchitecturyBuildSystemStep)
            .nextStep(::ArchitecturyProjectFilesStep)
            .nextStep(::ArchitecturyCommonMainClassStep)
            .nextStep(::ArchitecturyForgeMainClassStep)
            .nextStep(::ArchitecturyFabricMainClassStep)
            .nextStep(::ArchitecturyPostBuildSystemStep)
            .nextStep(::ArchitecturyReformatPackDescriptorStep)
    }

    class Factory : ModPlatformStep.Factory {
        override val name = "Architectury"
        override fun createStep(parent: ModPlatformStep) = ArchitecturyPlatformStep(parent)
    }
}

class ArchitecturyVersionChainStep(
    parent: NewProjectWizardStep,
    private val versionData: ArchitecturyVersionData,
) : AbstractMcVersionChainStep(
    parent,
    "Forge Version:",
    "Fabric Loader Version:",
    "Fabric API Version:",
    "Architectury API Version:",
) {
    companion object {
        private const val FORGE_VERSION = 1
        private const val FABRIC_LOADER_VERSION = 2
        private const val FABRIC_API_VERSION = 3
        private const val ARCHITECTURY_API_VERSION = 4

        val MC_VERSION_KEY =
            Key.create<SemanticVersion>("${ArchitecturyVersionChainStep::class.java.name}.mcVersion")
        val FORGE_VERSION_KEY =
            Key.create<SemanticVersion>("${ArchitecturyVersionChainStep::class.java.name}.forgeVersion")
        val FABRIC_LOADER_VERSION_KEY =
            Key.create<SemanticVersion>("${ArchitecturyVersionChainStep::class.java.name}.fabricLoaderVersion")
        val FABRIC_API_VERSION_KEY =
            Key.create<SemanticVersion>("${ArchitecturyVersionChainStep::class.java.name}.fabricApiVersion")
        val ARCHITECTURY_API_VERSION_KEY =
            Key.create<SemanticVersion>("${ArchitecturyVersionChainStep::class.java.name}.architecturyApiVersion")
    }

    private val mcVersions by lazy {
        versionData.architecturyVersions.versions.keys
            .intersect(versionData.forgeVersions.sortedMcVersions.toSet())
            .intersect(
                versionData.fabricVersions.game.mapNotNullTo(mutableSetOf()) {
                    SemanticVersion.tryParse(it.version)
                },
            )
            .toList()
    }

    private val useFabricApiProperty = propertyGraph.property(true)
        .bindBooleanStorage("${javaClass.name}.useFabricApi")
    private var useFabricApi by useFabricApiProperty

    private val useArchApiProperty = propertyGraph.property(true)
        .bindBooleanStorage("${javaClass.name}.useArchApi")
    private var useArchApi by useArchApiProperty

    override fun createComboBox(row: Row, index: Int, items: List<Comparable<*>>): Cell<VersionChainComboBox> {
        return when (index) {
            FABRIC_API_VERSION -> {
                val comboBox = super.createComboBox(row, index, items).bindEnabled(useFabricApiProperty)
                row.checkBox("Use Fabric API").bindSelected(useFabricApiProperty)
                row.label("").bindText(
                    getVersionProperty(MINECRAFT_VERSION).transform { mcVersion ->
                        val versionStr = mcVersion.toString()
                        val matched = versionData.fabricApiVersions.versions.any { versionStr in it.gameVersions }
                        if (matched) {
                            ""
                        } else {
                            "Unable to match API versions to Minecraft version"
                        }
                    },
                ).bindEnabled(useFabricApiProperty).component.foreground = JBColor.YELLOW
                comboBox
            }
            ARCHITECTURY_API_VERSION -> {
                val comboBox = super.createComboBox(row, index, items).bindEnabled(useArchApiProperty)
                row.checkBox("Use Architectury API").bindSelected(useArchApiProperty)
                comboBox
            }
            else -> super.createComboBox(row, index, items)
        }
    }

    override fun getAvailableVersions(versionsAbove: List<Comparable<*>>): List<Comparable<*>> {
        val mcVersion by lazy { versionsAbove[MINECRAFT_VERSION] as SemanticVersion }

        return when (versionsAbove.size) {
            MINECRAFT_VERSION -> mcVersions
            FORGE_VERSION -> versionData.forgeVersions.getForgeVersions(mcVersion)
            FABRIC_LOADER_VERSION -> versionData.fabricVersions.loader
            FABRIC_API_VERSION -> {
                val versionStr = mcVersion.toString()
                val apiVersions = versionData.fabricApiVersions.versions
                    .filter { versionStr in it.gameVersions }
                    .map { it.version }
                apiVersions.ifEmpty { versionData.fabricApiVersions.versions.map { it.version } }
            }
            ARCHITECTURY_API_VERSION -> versionData.architecturyVersions.getArchitecturyVersions(mcVersion)
            else -> throw IncorrectOperationException()
        }
    }

    override fun setupProject(project: Project) {
        super.setupProject(project)
        data.putUserData(MC_VERSION_KEY, getVersion(MINECRAFT_VERSION) as SemanticVersion)
        data.putUserData(FORGE_VERSION_KEY, getVersion(FORGE_VERSION) as SemanticVersion)
        data.putUserData(FABRIC_LOADER_VERSION_KEY, getVersion(FABRIC_LOADER_VERSION) as SemanticVersion)
        if (useFabricApi) {
            data.putUserData(FABRIC_API_VERSION_KEY, getVersion(FABRIC_API_VERSION) as SemanticVersion)
        }
        if (useArchApi) {
            data.putUserData(ARCHITECTURY_API_VERSION_KEY, getVersion(ARCHITECTURY_API_VERSION) as SemanticVersion)
        }
    }
}

class ArchitecturyOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::WebsiteStep)
        .nextStep(::RepositoryStep)
        .nextStep(::IssueTrackerStep)
}
