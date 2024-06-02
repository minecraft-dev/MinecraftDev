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

import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
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
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class FabricVersionsProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<FabricVersionsModel>(descriptor, graph, properties) {

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

    private var fabricVersions: FabricVersions? = null
    private var loomVersions: List<SemanticVersion>? = null
    private var fabricApiVersions: FabricApiVersions? = null

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
        panel.row("Minecraft Version:") {
            comboBox(mcVersionModel)
                .bindItem(mcVersionProperty)
                .validationRequestor(WHEN_GRAPH_PROPAGATION_FINISHED(graph))
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox("Show snapshots")
                .bindSelected(showMcSnapshotsProperty)
        }.enabled(descriptor.editable != false)

        panel.row("Loom Version:") {
            comboBox(loomVersionModel)
                .bindItem(loomVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)

        panel.row("Loader Version:") {
            comboBox(loaderVersionModel)
                .bindItem(loaderVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)

        panel.row("Yarn Version:") {
            comboBox(yarnVersionModel)
                .bindItem(yarnVersionProperty)
                .enabledIf(useOfficialMappingsProperty.not())
                .validationOnInput(BuiltinValidations.nonEmptyYarnVersion)
                .validationOnApply(BuiltinValidations.nonEmptyYarnVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox("Use official mappings")
                .bindSelected(useOfficialMappingsProperty)

            label("Unable to match Yarn versions to Minecraft version")
                .visibleIf(yarnHasMatchingGameVersion.not())
                .component.foreground = JBColor.YELLOW
        }.enabled(descriptor.editable != false)

        panel.row("FabricApi Version:") {
            comboBox(fabricApiVersionModel)
                .bindItem(fabricApiVersionProperty)
                .enabledIf(useFabricApiVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            checkBox("Use FabricApi")
                .bindSelected(useFabricApiVersionProperty)
            label("Unable to match API versions to Minecraft version")
                .visibleIf(fabricApiHasMatchingGameVersion.not())
                .component.foreground = JBColor.YELLOW
        }.enabled(descriptor.editable != false)
    }

    override fun setupProperty() {
        super.setupProperty()

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

        application.executeOnPooledThread {
            runBlocking {
                val fabricVersionsJob = asyncIO { FabricVersions.downloadData() }
                val loomVersionsJob = asyncIO {
                    collectMavenVersions("https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml")
                }
                val fabricApiVersionsJob = asyncIO { FabricApiVersions.downloadData() }

                this@FabricVersionsProperty.fabricVersions = fabricVersionsJob.await()
                this@FabricVersionsProperty.loomVersions = loomVersionsJob.await()
                    .mapNotNull(SemanticVersion::tryParse)
                    .sortedDescending()
                this@FabricVersionsProperty.fabricApiVersions = fabricApiVersionsJob.await()

                withContext(Dispatchers.Swing) {
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
                        val defaultValue = loomVersions.firstOrNull {
                            it.parts.none { it is SemanticVersion.Companion.VersionPart.PreReleasePart }
                        } ?: loomVersions.firstOrNull() ?: emptyVersion

                        loomVersionProperty.set(defaultValue)
                    }
                }
            }
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

    class Factory : CreatorPropertyFactory {

        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = FabricVersionsProperty(graph, descriptor, properties)
    }
}
