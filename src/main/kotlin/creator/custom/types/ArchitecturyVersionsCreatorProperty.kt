/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.MCDevBundle.invoke
import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.ArchitecturyVersionsModel
import com.demonwav.mcdev.platform.architectury.ArchitecturyVersion
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.platform.neoforge.version.NeoForgeVersion
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.util.application
import com.intellij.util.ui.AsyncProcessIcon
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class ArchitecturyVersionsCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<ArchitecturyVersionsModel>(descriptor, graph, properties, ArchitecturyVersionsModel::class.java) {

    private val emptyVersion = SemanticVersion.release()
    private val emptyValue = ArchitecturyVersionsModel(
        emptyVersion,
        emptyVersion,
        emptyVersion,
        emptyVersion,
        emptyVersion,
        FabricVersions.YarnVersion("", -1),
        true,
        emptyVersion,
        true,
        true,
        emptyVersion,
    )
    private val defaultValue = createDefaultValue(descriptor.default)

    private val loadingVersionsProperty = graph.property(true)
    override val graphProperty: GraphProperty<ArchitecturyVersionsModel> = graph.property(defaultValue)
    var model: ArchitecturyVersionsModel by graphProperty

    val mcVersionProperty = graphProperty.transform({ it.minecraft }, { model.copy(minecraft = it) })
    val mcVersionModel = DefaultComboBoxModel<SemanticVersion>()

    val forgeVersionProperty = graphProperty.transform({ it.forge }, { model.copy(forge = it) })
    val forgeVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    val isForgeAvailableProperty = forgeVersionProperty.transform { !it?.parts.isNullOrEmpty() }

    val nfVersionProperty = graphProperty.transform({ it.neoforge }, { model.copy(neoforge = it) })
    val nfVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    val isNfAvailableProperty = nfVersionProperty.transform { !it?.parts.isNullOrEmpty() }

    val loomVersionProperty = graphProperty.transform({ it.loom }, { model.copy(loom = it) })
    val loomVersionModel = DefaultComboBoxModel<SemanticVersion>()

    val loaderVersionProperty = graphProperty.transform({ it.loader }, { model.copy(loader = it) })
    val loaderVersionModel = DefaultComboBoxModel<SemanticVersion>()

    val yarnVersionProperty = graphProperty.transform({ it.yarn }, { model.copy(yarn = it) })
    val yarnVersionModel = DefaultComboBoxModel<FabricVersions.YarnVersion>()
    val yarnHasMatchingGameVersion = mcVersionProperty.transform { mcVersion ->
        val versions = fabricVersions
            ?: return@transform true
        val mcVersionString = mcVersion.toString()
        versions.mappings.any { it.gameVersion == mcVersionString }
    }

    val fabricApiVersionProperty = graphProperty.transform({ it.fabricApi }, { model.copy(fabricApi = it) })
    val fabricApiVersionModel = DefaultComboBoxModel<SemanticVersion>()
    val useFabricApiVersionProperty = graphProperty.transform({ it.useFabricApi }, { model.copy(useFabricApi = it) })
    val fabricApiHasMatchingGameVersion = mcVersionProperty.transform { mcVersion ->
        val apiVersions = fabricApiVersions
            ?: return@transform true
        val mcVersionString = mcVersion.toString()
        apiVersions.versions.any { mcVersionString in it.gameVersions }
    }

    val useOfficialMappingsProperty =
        graphProperty.transform({ it.useOfficialMappings }, { model.copy(useOfficialMappings = it) })

    val architecturyApiVersionProperty =
        graphProperty.transform({ it.architecturyApi }, { model.copy(architecturyApi = it) })
    val architecturyApiVersionModel = DefaultComboBoxModel<SemanticVersion>()
    val useArchitecturyApiVersionProperty =
        graphProperty.transform({ it.useArchitecturyApi }, { model.copy(useArchitecturyApi = it) })
    val architecturyApiHasMatchingGameVersion = mcVersionProperty.transform { mcVersion ->
        val apiVersions = architecturyVersions
            ?: return@transform true
        apiVersions.versions.containsKey(mcVersion)
    }

    override fun createDefaultValue(raw: Any?): ArchitecturyVersionsModel = when (raw) {
        is String -> deserialize(raw)
        else -> emptyValue
    }

    override fun serialize(value: ArchitecturyVersionsModel): String {
        return "${value.minecraft} ${value.forge} ${value.neoforge} ${value.loom} ${value.loader} ${value.yarn}" +
            " ${value.useFabricApi} ${value.fabricApi} ${value.useOfficialMappings} ${value.useArchitecturyApi}" +
            " ${value.architecturyApi}"
    }

    override fun deserialize(string: String): ArchitecturyVersionsModel {
        val segments = string.split(' ')
        val yarnSegments = segments.getOrNull(5)?.split(':')
        val yarnVersion = if (yarnSegments != null && yarnSegments.size == 2) {
            FabricVersions.YarnVersion(yarnSegments[0], yarnSegments[1].toInt())
        } else {
            emptyValue.yarn
        }
        return ArchitecturyVersionsModel(
            segments.getOrNull(0)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(1)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(2)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(3)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(4)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            yarnVersion,
            segments.getOrNull(6)?.toBoolean() != false,
            segments.getOrNull(7)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(8)?.toBoolean() != false,
            segments.getOrNull(9)?.toBoolean() != false,
            segments.getOrNull(10)?.let(SemanticVersion::tryParse) ?: emptyVersion,
        )
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row("") {
            cell(AsyncProcessIcon("ArchitecturyVersions download"))
            label(MCDevBundle("creator.ui.versions_download.label"))
        }.visibleIf(loadingVersionsProperty)

        panel.row("Minecraft Version:") {
            comboBox(mcVersionModel)
                .bindItem(mcVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        panel.row("Forge Version:") {
            comboBox(forgeVersionsModel)
                .bindItem(forgeVersionProperty)
                .enabledIf(isForgeAvailableProperty)
                .also { ComboboxSpeedSearch.installOn(it.component) }
                .component
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        panel.row("NeoForge Version:") {
            comboBox(nfVersionsModel)
                .bindItem(nfVersionProperty)
                .enabledIf(isNfAvailableProperty)
                .also { ComboboxSpeedSearch.installOn(it.component) }
                .component
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        //
        // panel.row("Loom Version:") {
        //     comboBox(loomVersionModel)
        //         .bindItem(loomVersionProperty)
        //         .validationOnInput(BuiltinValidations.nonEmptyVersion)
        //         .validationOnApply(BuiltinValidations.nonEmptyVersion)
        //         .also { ComboboxSpeedSearch.installOn(it.component) }
        // }.enabled(descriptor.editable != false)

        panel.row("Loader Version:") {
            comboBox(loaderVersionModel)
                .bindItem(loaderVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        // Official mappings forced currently, yarn mappings are not handled yet
        // panel.row("Yarn Version:") {
        //     comboBox(yarnVersionModel)
        //         .bindItem(yarnVersionProperty)
        //         .enabledIf(useOfficialMappingsProperty.not())
        //         .validationOnInput(BuiltinValidations.nonEmptyYarnVersion)
        //         .validationOnApply(BuiltinValidations.nonEmptyYarnVersion)
        //         .also { ComboboxSpeedSearch.installOn(it.component) }
        //
        //     checkBox("Use official mappings")
        //         .bindSelected(useOfficialMappingsProperty)
        //
        //     label("Unable to match Yarn versions to Minecraft version")
        //         .visibleIf(yarnHasMatchingGameVersion.not())
        //         .component.foreground = JBColor.YELLOW
        // }.enabled(descriptor.editable != false)

        panel.row("Fabric API Version:") {
            comboBox(fabricApiVersionModel)
                .bindItem(fabricApiVersionProperty)
                .enabledIf(useFabricApiVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox("Use Fabric API")
                .bindSelected(useFabricApiVersionProperty)
            label("Unable to match API versions to Minecraft version")
                .visibleIf(fabricApiHasMatchingGameVersion.not())
                .component.foreground = JBColor.YELLOW
        }.visibleIf(!loadingVersionsProperty)

        panel.row("Architectury API Version:") {
            comboBox(architecturyApiVersionModel)
                .bindItem(architecturyApiVersionProperty)
                .enabledIf(useArchitecturyApiVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox("Use Architectury API")
                .bindSelected(useArchitecturyApiVersionProperty)
            label("Unable to match API versions to Minecraft version")
                .visibleIf(architecturyApiHasMatchingGameVersion.not())
                .component.foreground = JBColor.YELLOW
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        var previousMcVersion: SemanticVersion? = null
        mcVersionProperty.afterChange { mcVersion ->
            if (previousMcVersion == mcVersion) {
                return@afterChange
            }

            previousMcVersion = mcVersion

            updateForgeVersions()
            updateNeoForgeVersions()
            updateYarnVersions()
            updateFabricApiVersions()
            updateArchitecturyApiVersions()
        }

        downloadVersions {
            val fabricVersions = fabricVersions
            if (fabricVersions != null) {
                loaderVersionModel.removeAllElements()
                loaderVersionModel.addAll(fabricVersions.loader)
                loaderVersionProperty.set(fabricVersions.loader.firstOrNull() ?: emptyVersion)
            }

            val loomVersions = loomVersions
            if (loomVersions != null) {
                loomVersionModel.removeAllElements()
                loomVersionModel.addAll(loomVersions)
                val defaultValue = loomVersions.find {
                    it.parts.any { it is SemanticVersion.Companion.VersionPart.PreReleasePart }
                } ?: loomVersions.firstOrNull() ?: emptyVersion

                loomVersionProperty.set(defaultValue)
            }

            updateMcVersionsList()

            loadingVersionsProperty.set(false)
        }
    }

    private fun updateMcVersionsList() {
        val architecturyVersions = architecturyVersions
            ?: return

        val mcVersions = architecturyVersions.versions.keys.sortedDescending()
        mcVersionModel.removeAllElements()
        mcVersionModel.addAll(mcVersions)

        val selectedMcVersion = when {
            mcVersionProperty.get() in mcVersions -> mcVersionProperty.get()
            defaultValue.minecraft in mcVersions -> defaultValue.minecraft
            else -> mcVersions.first()
        }
        mcVersionProperty.set(selectedMcVersion)
    }

    private fun updateForgeVersions() {
        val mcVersion = mcVersionProperty.get()

        val filterExpr = descriptor.parameters?.get("forgeMcVersionFilter") as? String
        if (filterExpr != null) {
            val conditionProps = mapOf("MC_VERSION" to mcVersion)
            if (!TemplateEvaluator.condition(conditionProps, filterExpr).getOrDefault(true)) {
                forgeVersionsModel.removeAllElements()
                application.invokeLater {
                    // For some reason we have to set those properties later for the values to actually be set
                    //  and the enabled state to be set appropriately
                    forgeVersionProperty.set(null)
                }
                return
            }
        }

        val availableForgeVersions = forgeVersions!!.getForgeVersions(mcVersion)
            .take(descriptor.limit ?: 50)
        forgeVersionsModel.removeAllElements()
        forgeVersionsModel.addAll(availableForgeVersions)
        application.invokeLater {
            forgeVersionProperty.set(availableForgeVersions.firstOrNull())
        }
    }

    private fun updateNeoForgeVersions() {
        val mcVersion = mcVersionProperty.get()

        val filterExpr = descriptor.parameters?.get("neoForgeMcVersionFilter") as? String
        if (filterExpr != null) {
            val conditionProps = mapOf("MC_VERSION" to mcVersion)
            if (!TemplateEvaluator.condition(conditionProps, filterExpr).getOrDefault(true)) {
                nfVersionsModel.removeAllElements()
                application.invokeLater {
                    nfVersionProperty.set(null)
                }
                return
            }
        }

        val availableNeoForgeVersions = neoForgeVersions!!.getNeoForgeVersions(mcVersion)
            .take(descriptor.limit ?: 50)
        nfVersionsModel.removeAllElements()
        nfVersionsModel.addAll(availableNeoForgeVersions)
        application.invokeLater {
            nfVersionProperty.set(availableNeoForgeVersions.firstOrNull())
        }
    }

    private fun updateYarnVersions() {
        val fabricVersions = fabricVersions
            ?: return

        val mcVersion = mcVersionProperty.get()
        val mcVersionString = mcVersion.toString()

        val yarnVersions = if (yarnHasMatchingGameVersion.get()) {
            fabricVersions.mappings.asSequence()
                .filter { it.gameVersion == mcVersionString }
                .map { it.version }
                .toList()
        } else {
            fabricVersions.mappings.map { it.version }
        }
        yarnVersionModel.removeAllElements()
        yarnVersionModel.addAll(yarnVersions)
        yarnVersionProperty.set(yarnVersions.firstOrNull() ?: emptyValue.yarn)
    }

    private fun updateFabricApiVersions() {
        val fabricApiVersions = fabricApiVersions
            ?: return

        val mcVersion = mcVersionProperty.get()
        val mcVersionString = mcVersion.toString()

        val apiVersions = if (fabricApiHasMatchingGameVersion.get()) {
            fabricApiVersions.versions.asSequence()
                .filter { mcVersionString in it.gameVersions }
                .map(FabricApiVersions.Version::version)
                .toList()
        } else {
            fabricApiVersions.versions.map(FabricApiVersions.Version::version)
        }
        fabricApiVersionModel.removeAllElements()
        fabricApiVersionModel.addAll(apiVersions)
        fabricApiVersionProperty.set(apiVersions.firstOrNull() ?: emptyVersion)
    }

    private fun updateArchitecturyApiVersions() {
        val architecturyVersions = architecturyVersions
            ?: return

        val mcVersion = mcVersionProperty.get()
        val availableArchitecturyApiVersions = architecturyVersions.getArchitecturyVersions(mcVersion)
        architecturyApiVersionModel.removeAllElements()
        architecturyApiVersionModel.addAll(availableArchitecturyApiVersions)

        architecturyApiVersionProperty.set(availableArchitecturyApiVersions.firstOrNull() ?: emptyVersion)
    }

    companion object {
        private var hasDownloadedVersions = false

        private var forgeVersions: ForgeVersion? = null
        private var neoForgeVersions: NeoForgeVersion? = null
        private var fabricVersions: FabricVersions? = null
        private var loomVersions: List<SemanticVersion>? = null
        private var fabricApiVersions: FabricApiVersions? = null
        private var architecturyVersions: ArchitecturyVersion? = null

        private fun downloadVersions(completeCallback: () -> Unit) {
            if (hasDownloadedVersions) {
                completeCallback()
                return
            }

            application.executeOnPooledThread {
                runBlocking {
                    awaitAll(
                        asyncIO { ForgeVersion.downloadData().also { forgeVersions = it } },
                        asyncIO { NeoForgeVersion.downloadData().also { neoForgeVersions = it } },
                        asyncIO { FabricVersions.downloadData().also { fabricVersions = it } },
                        asyncIO {
                            collectMavenVersions(
                                "https://maven.architectury.dev/dev/architectury/architectury-loom/maven-metadata.xml"
                            ).also {
                                loomVersions = it
                                    .mapNotNull(SemanticVersion::tryParse)
                                    .sortedDescending()
                            }
                        },
                        asyncIO { FabricApiVersions.downloadData().also { fabricApiVersions = it } },
                        asyncIO { ArchitecturyVersion.downloadData().also { architecturyVersions = it } },
                    )

                    hasDownloadedVersions = true

                    withContext(Dispatchers.Swing) {
                        completeCallback()
                    }
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {

        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = ArchitecturyVersionsCreatorProperty(descriptor, graph, properties)
    }
}
