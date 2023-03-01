/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.creator

import com.demonwav.mcdev.creator.platformtype.ModPlatformStep
import com.demonwav.mcdev.creator.step.AbstractCollapsibleStep
import com.demonwav.mcdev.creator.step.AbstractLatentStep
import com.demonwav.mcdev.creator.step.AbstractMcVersionChainStep
import com.demonwav.mcdev.creator.step.AuthorsStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.LicenseStep
import com.demonwav.mcdev.creator.step.ModNameStep
import com.demonwav.mcdev.creator.step.NewProjectWizardChainStep.Companion.nextStep
import com.demonwav.mcdev.creator.step.RepositoryStep
import com.demonwav.mcdev.creator.step.UseMixinsStep
import com.demonwav.mcdev.creator.step.VersionChainComboBox
import com.demonwav.mcdev.creator.step.WaitForSmartModeStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.inspections.sideonly.Side
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.demonwav.mcdev.util.bindEnabled
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.IncorrectOperationException
import kotlinx.coroutines.coroutineScope

class FabricPlatformStep(
    parent: ModPlatformStep,
) : AbstractLatentStep<Pair<FabricVersions, FabricApiVersions>>(parent) {
    override val description = "download Fabric versions"

    override suspend fun computeData() = coroutineScope {
        val fabricVersions = asyncIO { FabricVersions.downloadData() }
        val fabricApiVersions = asyncIO { FabricApiVersions.downloadData() }
        fabricVersions.await()?.let { a -> fabricApiVersions.await()?.let { b -> a to b } }
    }

    override fun createStep(data: Pair<FabricVersions, FabricApiVersions>): NewProjectWizardStep {
        val (fabricVersions, apiVersions) = data
        return FabricVersionChainStep(this, fabricVersions, apiVersions)
            .nextStep(::FabricEnvironmentStep)
            .nextStep(::UseMixinsStep)
            .nextStep(::ModNameStep)
            .nextStep(::LicenseStep)
            .nextStep(::FabricOptionalSettingsStep)
            .nextStep(::FabricBuildSystemStep)
            .nextStep(::FabricDumbModeFilesStep)
            .nextStep(::FabricPostBuildSystemStep)
            .nextStep(::WaitForSmartModeStep)
            .nextStep(::FabricSmartModeFilesStep)
    }

    class Factory : ModPlatformStep.Factory {
        override val name = "Fabric"
        override fun createStep(parent: ModPlatformStep) = FabricPlatformStep(parent)
    }
}

class FabricVersionChainStep(
    parent: NewProjectWizardStep,
    private val fabricVersions: FabricVersions,
    private val apiVersions: FabricApiVersions,
) : AbstractMcVersionChainStep(parent, "Loader Version:", "Yarn Version:", "API Version:") {
    companion object {
        private const val LOADER_VERSION = 1
        private const val YARN_VERSION = 2
        private const val API_VERSION = 3

        val MC_VERSION_KEY = Key.create<String>("${FabricVersionChainStep::class.java.name}.mcVersion")
        val LOADER_VERSION_KEY = Key.create<SemanticVersion>("${FabricVersionChainStep::class.java.name}.loaderVersion")
        val YARN_VERSION_KEY = Key.create<String>("${FabricVersionChainStep::class.java.name}.yarnVersion")
        val API_VERSION_KEY = Key.create<SemanticVersion>("${FabricVersionChainStep::class.java.name}.apiVersion")
        val OFFICIAL_MAPPINGS_KEY = Key.create<Boolean>("${FabricVersionChainStep::class.java.name}.officialMappings")
    }

    private val showSnapshotsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.showSnapshots")
    private var showSnapshots by showSnapshotsProperty

    private val useApiProperty = propertyGraph.property(true)
        .bindBooleanStorage("${javaClass.name}.useApi")
    private var useApi by useApiProperty

    private val useOfficialMappingsProperty = propertyGraph.property(false)
        .bindBooleanStorage("${javaClass.name}.useOfficialMappings")
    private var useOfficialMappings by useOfficialMappingsProperty

    init {
        showSnapshotsProperty.afterChange { updateVersionBox() }
    }

    private val mcVersions by lazy {
        fabricVersions.game.mapIndexed { index, version ->
            FabricMcVersion(fabricVersions.game.size - 1 - index, version.version, version.stable)
        }
    }

    override fun createComboBox(row: Row, index: Int, items: List<Comparable<*>>): Cell<VersionChainComboBox> {
        return when (index) {
            MINECRAFT_VERSION -> {
                val comboBox = super.createComboBox(row, index, items)
                row.checkBox("Show snapshots").bindSelected(showSnapshotsProperty)
                comboBox
            }
            YARN_VERSION -> {
                val comboBox = super.createComboBox(row, index, items).bindEnabled(useOfficialMappingsProperty.not())
                row.checkBox("Use Official Mappings").bindSelected(useOfficialMappingsProperty)
                row.label("").bindText(
                    getVersionProperty(MINECRAFT_VERSION).transform { mcVersion ->
                        mcVersion as FabricMcVersion
                        val matched = fabricVersions.mappings.any { it.gameVersion == mcVersion.version }
                        if (matched) {
                            ""
                        } else {
                            "Unable to match Yarn versions to Minecraft version"
                        }
                    },
                ).bindEnabled(useOfficialMappingsProperty.not()).component.foreground = JBColor.YELLOW
                comboBox
            }
            API_VERSION -> {
                val comboBox = super.createComboBox(row, index, items).bindEnabled(useApiProperty)
                row.checkBox("Use Fabric API").bindSelected(useApiProperty)
                row.label("").bindText(
                    getVersionProperty(MINECRAFT_VERSION).transform { mcVersion ->
                        mcVersion as FabricMcVersion
                        val matched = apiVersions.versions.any { mcVersion.version in it.gameVersions }
                        if (matched) {
                            ""
                        } else {
                            "Unable to match API versions to Minecraft version"
                        }
                    },
                ).bindEnabled(useApiProperty).component.foreground = JBColor.YELLOW
                comboBox
            }
            else -> super.createComboBox(row, index, items)
        }
    }

    override fun getAvailableVersions(versionsAbove: List<Comparable<*>>): List<Comparable<*>> {
        return when (versionsAbove.size) {
            MINECRAFT_VERSION -> mcVersions
            LOADER_VERSION -> fabricVersions.loader
            YARN_VERSION -> {
                val mcVersion = versionsAbove[MINECRAFT_VERSION] as FabricMcVersion
                val filteredVersions = fabricVersions.mappings.mapNotNull { mapping ->
                    mapping.version.takeIf { mapping.gameVersion == mcVersion.version }
                }
                filteredVersions.ifEmpty { fabricVersions.mappings.map { it.version } }
            }
            API_VERSION -> {
                val mcVersion = versionsAbove[MINECRAFT_VERSION] as FabricMcVersion
                val filteredVersions = apiVersions.versions.mapNotNull { api ->
                    api.version.takeIf { mcVersion.version in api.gameVersions }
                }
                filteredVersions.ifEmpty { apiVersions.versions.map { it.version } }
            }
            else -> throw IncorrectOperationException()
        }
    }

    override fun setupUI(builder: Panel) {
        super.setupUI(builder)
        if (!showSnapshots) {
            updateVersionBox()
        }
    }

    private fun updateVersionBox() {
        val versionBox = getVersionBox(MINECRAFT_VERSION) ?: return
        val selectedItem = versionBox.selectedItem
        versionBox.setSelectableItems(mcVersions.filter { gameVer -> showSnapshots || gameVer.stable })
        versionBox.selectedItem = selectedItem
    }

    override fun setupProject(project: Project) {
        super.setupProject(project)
        data.putUserData(MC_VERSION_KEY, (getVersion(MINECRAFT_VERSION) as FabricMcVersion).version)
        data.putUserData(LOADER_VERSION_KEY, getVersion(LOADER_VERSION) as SemanticVersion)
        data.putUserData(YARN_VERSION_KEY, (getVersion(YARN_VERSION) as FabricVersions.YarnVersion).name)
        if (useApi) {
            data.putUserData(API_VERSION_KEY, getVersion(API_VERSION) as SemanticVersion)
        }
        data.putUserData(OFFICIAL_MAPPINGS_KEY, useOfficialMappings)
    }
}

class FabricEnvironmentStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val environmentProperty = propertyGraph.property(Side.NONE)
    init {
        environmentProperty.transform(Side::name, Side::valueOf).bindStorage("${javaClass.name}.side")
    }
    private var environment by environmentProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row("Environment:") {
                comboBox(listOf("Both", "Client", "Server"))
                    .bindItem(
                        environmentProperty.transform({
                            when (it) {
                                Side.CLIENT -> "Client"
                                Side.SERVER -> "Server"
                                else -> "Both"
                            }
                        }, {
                            when (it) {
                                "Client" -> Side.CLIENT
                                "Server" -> Side.SERVER
                                else -> Side.NONE
                            }
                        },),
                    )
            }
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, environment)
    }

    companion object {
        val KEY = Key.create<Side>("${FabricEnvironmentStep::class.java.name}.environment")
    }
}

class FabricOptionalSettingsStep(parent: NewProjectWizardStep) : AbstractCollapsibleStep(parent) {
    override val title = "Optional Settings"

    override fun createStep() = DescriptionStep(this)
        .nextStep(::AuthorsStep)
        .nextStep(::WebsiteStep)
        .nextStep(::RepositoryStep)
}
