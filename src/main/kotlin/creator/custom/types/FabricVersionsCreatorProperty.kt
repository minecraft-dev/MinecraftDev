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
import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.FabricVersionsModel
import com.demonwav.mcdev.platform.fabric.util.FabricApiVersions
import com.demonwav.mcdev.platform.fabric.util.FabricVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.validation.WHEN_GRAPH_PROPAGATION_FINISHED
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

class FabricVersionsCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<FabricVersionsModel>(descriptor, graph, properties, FabricVersionsModel::class.java) {

    private val emptyVersion = SemanticVersion.release()
    private val emptyValue = FabricVersionsModel(
        emptyVersion,
        emptyVersion,
        emptyVersion,
        FabricVersions.YarnVersion("", -1),
        true,
        emptyVersion,
        false,
    )
    private val defaultValue = createDefaultValue(descriptor.default)

    private val loadingVersionsProperty = graph.property(true)
    override val graphProperty: GraphProperty<FabricVersionsModel> = graph.property(defaultValue)
    var model: FabricVersionsModel by graphProperty

    val mcVersionProperty = graphProperty.transform({ it.minecraftVersion }, { model.copy(minecraftVersion = it) })
    val mcVersionModel = DefaultComboBoxModel<SemanticVersion>()
    val showMcSnapshotsProperty = graph.property(false)
        .bindBooleanStorage(makeStorageKey("showMcSnapshots"))

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

    override fun createDefaultValue(raw: Any?): FabricVersionsModel = when (raw) {
        is String -> deserialize(raw)
        else -> emptyValue
    }

    override fun serialize(value: FabricVersionsModel): String {
        return "${value.minecraftVersion} ${value.loom} ${value.loader} ${value.yarn}" +
            " ${value.useFabricApi} ${value.fabricApi} ${value.useOfficialMappings}"
    }

    override fun deserialize(string: String): FabricVersionsModel {
        val segments = string.split(' ')
        val yarnSegments = segments.getOrNull(3)?.split(':')
        val yarnVersion = if (yarnSegments != null && yarnSegments.size == 2) {
            FabricVersions.YarnVersion(yarnSegments[0], yarnSegments[1].toInt())
        } else {
            emptyValue.yarn
        }
        return FabricVersionsModel(
            segments.getOrNull(0)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(1)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(2)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            yarnVersion,
            segments.getOrNull(4).toBoolean(),
            segments.getOrNull(5)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(6).toBoolean(),
        )
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row("") {
            cell(AsyncProcessIcon("FabricVersions download"))
            label(MCDevBundle("creator.ui.versions_download.label"))
        }.visibleIf(loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.mc_version.label")) {
            comboBox(mcVersionModel)
                .bindItem(mcVersionProperty)
                .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox(MCDevBundle("creator.ui.show_snapshots.label"))
                .bindSelected(showMcSnapshotsProperty)
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.loom_version.label")) {
            comboBox(loomVersionModel)
                .bindItem(loomVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.loader_version.label")) {
            comboBox(loaderVersionModel)
                .bindItem(loaderVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.yarn_version.label")) {
            comboBox(yarnVersionModel)
                .bindItem(yarnVersionProperty)
                .enabledIf(useOfficialMappingsProperty.not())
                .validationOnInput(BuiltinValidations.nonEmptyYarnVersion)
                .validationOnApply(BuiltinValidations.nonEmptyYarnVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox(MCDevBundle("creator.ui.use_official_mappings.label"))
                .bindSelected(useOfficialMappingsProperty)

            label(MCDevBundle("creator.ui.warn.no_yarn_to_mc_match"))
                .visibleIf(yarnHasMatchingGameVersion.not())
                .component.foreground = JBColor.YELLOW
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.fabricapi_version.label")) {
            comboBox(fabricApiVersionModel)
                .bindItem(fabricApiVersionProperty)
                .enabledIf(useFabricApiVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox(MCDevBundle("creator.ui.use_fabricapi.label"))
                .bindSelected(useFabricApiVersionProperty)
            label(MCDevBundle("creator.ui.warn.no_fabricapi_to_mc_match"))
                .visibleIf(fabricApiHasMatchingGameVersion.not())
                .component.foreground = JBColor.YELLOW
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        showMcSnapshotsProperty.afterChange { updateMcVersionsList() }

        var previousMcVersion: SemanticVersion? = null
        mcVersionProperty.afterChange { mcVersion ->
            if (previousMcVersion == mcVersion) {
                return@afterChange
            }

            previousMcVersion = mcVersion
            updateYarnVersions()
            updateFabricApiVersions()
        }

        downloadVersion {
            val fabricVersions = fabricVersions
            if (fabricVersions != null) {
                loaderVersionModel.removeAllElements()
                loaderVersionModel.addAll(fabricVersions.loader)
                loaderVersionProperty.set(fabricVersions.loader.firstOrNull() ?: emptyVersion)

                updateMcVersionsList()
            }

            val loomVersions = loomVersions
            if (loomVersions != null) {
                loomVersionModel.removeAllElements()
                loomVersionModel.addAll(loomVersions)
                val defaultValue = loomVersions.firstOrNull { it.toString().endsWith("-SNAPSHOT") }
                    ?: loomVersions.firstOrNull()
                    ?: emptyVersion

                loomVersionProperty.set(defaultValue)
            }

            loadingVersionsProperty.set(false)
        }
    }

    private fun updateMcVersionsList() {
        val versions = fabricVersions
            ?: return

        val showSnapshots = showMcSnapshotsProperty.get()
        val mcVersions = versions.game.asSequence()
            .filter { showSnapshots || it.stable }
            .mapNotNull { version -> SemanticVersion.tryParse(version.version) }
            .toList()

        mcVersionModel.removeAllElements()
        mcVersionModel.addAll(mcVersions)
        mcVersionProperty.set(mcVersions.firstOrNull() ?: emptyVersion)
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

    companion object {
        private var hasDownloadedVersions = false

        private var fabricVersions: FabricVersions? = null
        private var loomVersions: List<SemanticVersion>? = null
        private var fabricApiVersions: FabricApiVersions? = null

        private fun downloadVersion(uiCallback: () -> Unit) {
            if (hasDownloadedVersions) {
                uiCallback()
                return
            }

            application.executeOnPooledThread {
                runBlocking {
                    awaitAll(
                        asyncIO { FabricVersions.downloadData().also { fabricVersions = it } },
                        asyncIO {
                            collectMavenVersions(
                                "https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml"
                            ).mapNotNull(SemanticVersion::tryParse)
                                .sortedDescending()
                                .also { loomVersions = it }
                        },
                        asyncIO { FabricApiVersions.downloadData().also { fabricApiVersions = it } },
                    )

                    hasDownloadedVersions = true

                    withContext(Dispatchers.Swing) {
                        uiCallback()
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
        ): CreatorProperty<*> = FabricVersionsCreatorProperty(descriptor, graph, properties)
    }
}
