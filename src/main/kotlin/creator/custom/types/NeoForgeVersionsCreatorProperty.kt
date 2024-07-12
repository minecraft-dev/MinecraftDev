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
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.NeoForgeVersions
import com.demonwav.mcdev.platform.neoforge.version.NeoForgeVersion
import com.demonwav.mcdev.platform.neoforge.version.NeoGradleVersion
import com.demonwav.mcdev.platform.neoforge.version.platform.neoforge.version.NeoModDevVersion
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.asyncIO
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.util.application
import com.intellij.util.ui.AsyncProcessIcon
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class NeoForgeVersionsCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<NeoForgeVersions>(descriptor, graph, properties, NeoForgeVersions::class.java) {

    private val emptyVersion = SemanticVersion.release()

    private val defaultValue = createDefaultValue(descriptor.default)

    private val loadingVersionsProperty = graph.property(true)
    override val graphProperty: GraphProperty<NeoForgeVersions> = graph.property(defaultValue)
    var versions: NeoForgeVersions by graphProperty

    private var previousMcVersion: SemanticVersion? = null

    private val mcVersionProperty = graphProperty.transform({ it.minecraft }, { versions.copy(minecraft = it) })
    private val mcVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val nfVersionProperty = graphProperty.transform({ it.neoforge }, { versions.copy(neoforge = it) })
    private val nfVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val ngVersionProperty = graphProperty.transform({ it.neogradle }, { versions.copy(neogradle = it) })
    private val mdVersionProperty = graphProperty.transform({ it.moddev }, { versions.copy(moddev = it) })

    override fun createDefaultValue(raw: Any?): NeoForgeVersions {
        if (raw is String) {
            return deserialize(raw)
        }

        return NeoForgeVersions(emptyVersion, emptyVersion, emptyVersion, emptyVersion)
    }

    override fun serialize(value: NeoForgeVersions): String {
        return "${value.minecraft} ${value.neoforge} ${value.neogradle} ${value.moddev}"
    }

    override fun deserialize(string: String): NeoForgeVersions {
        val versions = string.split(' ')
            .take(4)
            .map { SemanticVersion.tryParse(it) ?: emptyVersion }

        return NeoForgeVersions(
            versions.getOrNull(0) ?: emptyVersion,
            versions.getOrNull(1) ?: emptyVersion,
            versions.getOrNull(2) ?: emptyVersion,
            versions.getOrNull(3) ?: emptyVersion,
        )
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row("") {
            cell(AsyncProcessIcon("NeoForgeVersions download"))
            label(MCDevBundle("creator.ui.versions_download.label"))
        }.visibleIf(loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.mc_version.label")) {
            comboBox(mcVersionsModel)
                .bindItem(mcVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            label(MCDevBundle("creator.ui.neoforge_version.label")).gap(RightGap.SMALL)
            comboBox(nfVersionsModel)
                .bindItem(nfVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
            .visibleIf(!loadingVersionsProperty)
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        mcVersionProperty.afterChange { mcVersion ->
            if (mcVersion == previousMcVersion) {
                return@afterChange
            }

            previousMcVersion = mcVersion
            val availableNfVersions = nfVersion!!.getNeoForgeVersions(mcVersion)
                .take(descriptor.limit ?: 50)
            nfVersionsModel.removeAllElements()
            nfVersionsModel.addAll(availableNfVersions)
            nfVersionProperty.set(availableNfVersions.firstOrNull() ?: emptyVersion)
        }

        val mcVersionFilter = descriptor.parameters?.get("mcVersionFilter") as? String
        downloadVersion(mcVersionFilter) {
            val mcVersions = mcVersions ?: return@downloadVersion

            mcVersionsModel.removeAllElements()
            mcVersionsModel.addAll(mcVersions)

            val selectedMcVersion = when {
                mcVersionProperty.get() in mcVersions -> mcVersionProperty.get()
                defaultValue.minecraft in mcVersions -> defaultValue.minecraft
                else -> mcVersions.first()
            }
            mcVersionProperty.set(selectedMcVersion)

            ngVersionProperty.set(ngVersion?.versions?.firstOrNull() ?: emptyVersion)
            mdVersionProperty.set(mdVersion?.versions?.firstOrNull() ?: emptyVersion)

            loadingVersionsProperty.set(false)
        }
    }

    companion object {

        private var hasDownloadedVersions = false

        private var nfVersion: NeoForgeVersion? = null
        private var ngVersion: NeoGradleVersion? = null
        private var mdVersion: NeoModDevVersion? = null
        private var mcVersions: List<SemanticVersion>? = null

        private fun downloadVersion(mcVersionFilter: String?, uiCallback: () -> Unit) {
            if (hasDownloadedVersions) {
                uiCallback()
                return
            }

            application.executeOnPooledThread {
                runBlocking {
                    awaitAll(
                        asyncIO { NeoForgeVersion.downloadData().also { nfVersion = it } },
                        asyncIO { NeoGradleVersion.downloadData().also { ngVersion = it } },
                        asyncIO { NeoModDevVersion.downloadData().also { mdVersion = it } },
                    )

                    mcVersions = nfVersion?.sortedMcVersions?.let { mcVersion ->
                        if (mcVersionFilter != null) {
                            mcVersion.filter { version ->
                                val conditionProps = mapOf("MC_VERSION" to version)
                                TemplateEvaluator.condition(conditionProps, mcVersionFilter).getOrDefault(true)
                            }
                        } else {
                            mcVersion
                        }
                    }

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
        ): CreatorProperty<*> = NeoForgeVersionsCreatorProperty(descriptor, graph, properties)
    }
}
