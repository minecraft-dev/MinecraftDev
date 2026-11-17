/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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
import com.demonwav.mcdev.creator.ParchmentVersion
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.HasMinecraftVersion
import com.demonwav.mcdev.creator.custom.model.ParchmentVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParchmentCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    context: CreatorContext
) : CreatorProperty<ParchmentVersions>(descriptor, context, ParchmentVersions::class.java) {

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

        return ParchmentVersions(true, emptyVersion, emptyVersion,
            includeOlderMcVersions = false,
            includeSnapshots = false
        )
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

    override fun buildUi(panel: Panel) {
        panel.row(descriptor.translatedLabel) {
            @Suppress("DialogTitleCapitalization")
            checkBox(MCDevBundle("creator.ui.parchment.use.label"))
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

        panel.row(MCDevBundle("creator.ui.parchment.include.label")) {
            checkBox(MCDevBundle("creator.ui.parchment.include.older_mc_versions"))
                .bindSelected(includeOlderMcVersionsProperty)
                .enabledIf(useParchmentProperty)

            checkBox(MCDevBundle("creator.ui.parchment.include.snapshots"))
                .bindSelected(includeSnapshotsProperty)
                .enabledIf(useParchmentProperty)
        }.enabled(descriptor.editable != false)
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        val platformMcVersionPropertyName = descriptor.parameters?.get("minecraftVersionProperty") as? String
        val platformMcVersionProperty = properties[platformMcVersionPropertyName]
        platformMcVersionProperty?.graphProperty?.afterChange {
            val minecraftVersion = getPlatformMinecraftVersion()
            if (mcVersionsModel.getIndexOf(minecraftVersion) == -1) {
                refreshVersionsLists(forceLatestVersions = true)
            } else if (minecraftVersion != null) {
                graphProperty.set(graphProperty.get().copy(minecraftVersion = minecraftVersion))
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

        downloadVersions(context) {
            refreshVersionsLists()
        }
    }

    private fun refreshVersionsLists(updateMcVersions: Boolean = true, forceLatestVersions: Boolean = false) {
        val includeOlderMcVersions = includeOlderMcVersionsProperty.get() || forceLatestVersions
        val includeSnapshots = includeSnapshotsProperty.get() || forceLatestVersions

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

            if (forceLatestVersions) {
                mcVersionProperty.set(mcVersions.maxOrNull() ?: emptyVersion)
            } else {
                val selectedMcVersion = when {
                    mcVersionProperty.get() in mcVersions -> mcVersionProperty.get()
                    defaultValue.minecraftVersion in mcVersions -> defaultValue.minecraftVersion
                    else -> {
                        refreshVersionsLists(forceLatestVersions = true)
                        return
                        // getPlatformMinecraftVersion() ?: mcVersions.first()
                    }
                }

                if (mcVersionProperty.get() != selectedMcVersion) {
                    mcVersionProperty.set(selectedMcVersion)
                }
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
        if (forceLatestVersions) {
            versionProperty.set(parchmentVersions.maxOrNull() ?: emptyVersion)
        } else {
            versionProperty.set(parchmentVersions.firstOrNull() ?: emptyVersion)
        }
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

        internal fun clearCache() {
            hasDownloadedVersions = false
            allParchmentVersions = null
        }

        private fun downloadVersions(context: CreatorContext, uiCallback: () -> Unit) {
            if (hasDownloadedVersions) {
                uiCallback()
                return
            }

            val scope = context.childScope("ParchmentCreatorProperty")
            scope.launch(Dispatchers.IO) {
                allParchmentVersions = ParchmentVersion.downloadData()
                    .sortedByDescending(ParchmentVersion::parchmentVersion)

                hasDownloadedVersions = true

                withContext(context.uiContext) {
                    uiCallback()
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            context: CreatorContext
        ): CreatorProperty<*> = ParchmentCreatorProperty(descriptor, context)
    }
}
