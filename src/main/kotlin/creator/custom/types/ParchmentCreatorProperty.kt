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

import com.demonwav.mcdev.creator.ParchmentVersion
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.HasMinecraftVersion
import com.demonwav.mcdev.creator.custom.model.ParchmentVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.util.application
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class ParchmentCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<ParchmentVersions>(descriptor, graph, properties, ParchmentVersions::class.java) {

    private val emptyVersion = SemanticVersion.release()

    private val defaultValue = createDefaultValue(descriptor.default)

    override val graphProperty: GraphProperty<ParchmentVersions> = graph.property(defaultValue)
    var versions: ParchmentVersions by graphProperty

    private var availableParchmentVersions: List<ParchmentVersion> = emptyList()

    private val useParchmentProperty = graphProperty.transform({ it.use }, { versions.copy(use = it) })
    private val versionProperty = graphProperty.transform({ it.version }, { versions.copy(version = it) })
    private val versionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val mcVersionProperty =
        graphProperty.transform({ it.minecraftVersion }, { versions.copy(minecraftVersion = it) })
    private val mcVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val includeOlderMcVersionsProperty =
        graphProperty.transform({ it.includeOlderMcVersions }, { versions.copy(includeOlderMcVersions = it) })
    private val includeSnapshotsProperty =
        graphProperty.transform({ it.includeSnapshots }, { versions.copy(includeSnapshots = it) })

    override fun createDefaultValue(raw: Any?): ParchmentVersions {
        if (raw is String) {
            return deserialize(raw)
        }

        return ParchmentVersions(true, emptyVersion, emptyVersion, false, false)
    }

    override fun serialize(value: ParchmentVersions): String {
        return "${value.use} ${value.version} ${value.minecraftVersion}" +
            " ${value.includeOlderMcVersions} ${value.includeSnapshots}"
    }

    override fun deserialize(string: String): ParchmentVersions {
        val segments = string.split(' ')
        return ParchmentVersions(
            segments.getOrNull(0)?.toBoolean() ?: true,
            segments.getOrNull(1)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(2)?.let(SemanticVersion::tryParse) ?: emptyVersion,
            segments.getOrNull(3).toBoolean(),
            segments.getOrNull(4).toBoolean(),
        )
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            checkBox("Use Parchment")
                .bindSelected(useParchmentProperty)

            comboBox(mcVersionsModel)
                .bindItem(mcVersionProperty)
                .enabledIf(useParchmentProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            comboBox(versionsModel)
                .bindItem(versionProperty)
                .enabledIf(useParchmentProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)

        panel.row("Include") {
            checkBox("Older Minecraft versions")
                .bindSelected(includeOlderMcVersionsProperty)
                .enabledIf(useParchmentProperty)

            checkBox("Snapshots")
                .bindSelected(includeSnapshotsProperty)
                .enabledIf(useParchmentProperty)
        }.enabled(descriptor.editable != false)
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        val platformMcVersionPropertyName = descriptor.parameters?.get("minecraftVersionProperty") as? String
        val platformMcVersionProperty = properties[platformMcVersionPropertyName]
        if (platformMcVersionProperty != null) {
            graphProperty.dependsOn(platformMcVersionProperty.graphProperty, true) {
                val minecraftVersion = getPlatformMinecraftVersion()
                if (minecraftVersion != null) {
                    graphProperty.get().copy(minecraftVersion = minecraftVersion)
                } else {
                    graphProperty.get()
                }
            }
        }

        var previousMcVersion: SemanticVersion? = null
        mcVersionProperty.afterChange { mcVersion ->
            if (mcVersion == previousMcVersion) {
                return@afterChange
            }

            previousMcVersion = mcVersion
            refreshVersionsLists(updateMcVersions = false)
        }

        var previousOlderMcVersions: Boolean? = null
        includeOlderMcVersionsProperty.afterChange { newValue ->
            if (previousOlderMcVersions == newValue) {
                return@afterChange
            }

            previousOlderMcVersions = newValue
            refreshVersionsLists()
        }

        var previousIncludeSnapshots: Boolean? = null
        includeSnapshotsProperty.afterChange { newValue ->
            if (previousIncludeSnapshots == newValue) {
                return@afterChange
            }

            previousIncludeSnapshots = newValue
            refreshVersionsLists()
        }

        downloadVersions {
            refreshVersionsLists()

            val minecraftVersion = getPlatformMinecraftVersion()
            if (minecraftVersion != null) {
                mcVersionProperty.set(minecraftVersion)
            }
        }
    }

    private fun refreshVersionsLists(updateMcVersions: Boolean = true) {
        val includeOlderMcVersions = includeOlderMcVersionsProperty.get()
        val includeSnapshots = includeSnapshotsProperty.get()

        if (updateMcVersions) {
            val platformMcVersion = getPlatformMinecraftVersion()
            availableParchmentVersions = allParchmentVersions
                ?.filter { version ->
                    if (!includeOlderMcVersions && platformMcVersion != null && version.mcVersion < platformMcVersion) {
                        return@filter false
                    }

                    if (!includeSnapshots && version.parchmentVersion.contains("-SNAPSHOT")) {
                        return@filter false
                    }

                    return@filter true
                }
                ?: return

            val mcVersions = availableParchmentVersions.mapTo(mutableSetOf(), ParchmentVersion::mcVersion)
            mcVersionsModel.removeAllElements()
            mcVersionsModel.addAll(mcVersions)

            val selectedMcVersion = when {
                mcVersionProperty.get() in mcVersions -> mcVersionProperty.get()
                defaultValue.minecraftVersion in mcVersions -> defaultValue.minecraftVersion
                else -> getPlatformMinecraftVersion() ?: mcVersions.first()
            }

            if (mcVersionProperty.get() != selectedMcVersion) {
                mcVersionProperty.set(selectedMcVersion)
            }
        }

        val selectedMcVersion = mcVersionProperty.get()
        val parchmentVersions = availableParchmentVersions.asSequence()
            .filter { it.mcVersion == selectedMcVersion }
            .mapNotNull { SemanticVersion.tryParse(it.parchmentVersion) }
            .sortedDescending()
            .toList()
        versionsModel.removeAllElements()
        versionsModel.addAll(parchmentVersions)
        versionProperty.set(parchmentVersions.firstOrNull() ?: emptyVersion)
    }

    private fun getPlatformMinecraftVersion(): SemanticVersion? {
        val platformMcVersionPropertyName = descriptor.parameters?.get("minecraftVersionProperty") as? String
        val platformMcVersionProperty = properties[platformMcVersionPropertyName]

        val version = when (val version = platformMcVersionProperty?.get()) {
            is SemanticVersion -> version
            is HasMinecraftVersion -> version.minecraftVersion
            else -> return null
        }

        // Ensures we get no trailing .0 for the first major version (1.21.0 -> 1.21)
        // This is required because otherwise those versions won't be properly compared against Parchment's
        val normalizedVersion = version.parts.dropLastWhile { part ->
            part is SemanticVersion.Companion.VersionPart.ReleasePart && part.version == 0
        }

        return SemanticVersion(normalizedVersion)
    }

    companion object {

        private var hasDownloadedVersions = false

        private var allParchmentVersions: List<ParchmentVersion>? = null

        private fun downloadVersions(uiCallback: () -> Unit) {
            if (hasDownloadedVersions) {
                uiCallback()
                return
            }

            application.executeOnPooledThread {
                runBlocking {
                    allParchmentVersions = ParchmentVersion.downloadData()
                        .sortedByDescending(ParchmentVersion::parchmentVersion)

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
        ): CreatorProperty<*> = ParchmentCreatorProperty(descriptor, graph, properties)
    }
}
