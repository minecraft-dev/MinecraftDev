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
import com.demonwav.mcdev.creator.custom.model.ForgeVersions
import com.demonwav.mcdev.platform.forge.version.ForgeVersion
import com.demonwav.mcdev.util.SemanticVersion
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
import kotlin.collections.Map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class ForgeVersionsCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<ForgeVersions>(descriptor, graph, properties, ForgeVersions::class.java) {

    private val emptyVersion = SemanticVersion.release()

    private val defaultValue = createDefaultValue(descriptor.default)

    private val loadingVersionsProperty = graph.property(true)
    override val graphProperty: GraphProperty<ForgeVersions> = graph.property(defaultValue)
    var versions: ForgeVersions by graphProperty

    private var previousMcVersion: SemanticVersion? = null

    private val mcVersionProperty = graphProperty.transform({ it.minecraft }, { versions.copy(minecraft = it) })
    private val mcVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val forgeVersionProperty = graphProperty.transform({ it.forge }, { versions.copy(forge = it) })
    private val forgeVersionsModel = DefaultComboBoxModel<SemanticVersion>()

    private var mcVersionFilterParents: List<String>? = null

    override fun createDefaultValue(raw: Any?): ForgeVersions {
        if (raw is String) {
            return deserialize(raw)
        }

        return ForgeVersions(emptyVersion, emptyVersion)
    }

    override fun serialize(value: ForgeVersions): String {
        return "${value.minecraft} ${value.forge}"
    }

    override fun deserialize(string: String): ForgeVersions {
        val versions = string.split(' ')
            .take(2)
            .map { SemanticVersion.tryParse(it) ?: emptyVersion }

        return ForgeVersions(
            versions.getOrNull(0) ?: emptyVersion,
            versions.getOrNull(1) ?: emptyVersion,
        )
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row("") {
            cell(AsyncProcessIcon("ForgeVersions download"))
            label(MCDevBundle("creator.ui.versions_download.label"))
        }.visibleIf(loadingVersionsProperty)

        panel.row(MCDevBundle("creator.ui.mc_version.label")) {
            comboBox(mcVersionsModel)
                .bindItem(mcVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            label(MCDevBundle("creator.ui.forge_version.label")).gap(RightGap.SMALL)
            comboBox(forgeVersionsModel)
                .bindItem(forgeVersionProperty)
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
            val availableForgeVersions = forgeVersion!!.getForgeVersions(mcVersion)
                .take(descriptor.limit ?: 50)
            forgeVersionsModel.removeAllElements()
            forgeVersionsModel.addAll(availableForgeVersions)
            forgeVersionProperty.set(availableForgeVersions.firstOrNull() ?: emptyVersion)
        }

        descriptor.parameters?.get("mcVersionFilterParents")?.let { parents ->
            if (parents !is List<*> || parents.any { it !is String }) {
                reporter.error("mcVersionFilterParents must be a list of strings")
            } else {
                @Suppress("UNCHECKED_CAST")
                this.mcVersionFilterParents = parents as List<String>
                for (parent in parents) {
                    val parentProp = properties[parent]
                    if (parentProp == null) {
                        reporter.error("Unknown mcVersionFilter parent $parent")
                        continue
                    }

                    parentProp.graphProperty.afterChange {
                        reloadMinecraftVersions()
                    }
                }
            }
        }

        downloadVersions {
            reloadMinecraftVersions()

            loadingVersionsProperty.set(false)
        }
    }

    private fun reloadMinecraftVersions() {
        val forgeVersions = forgeVersion
            ?: return

        val filterExpr = descriptor.parameters?.get("mcVersionFilter") as? String
        val mcVersions = if (filterExpr != null) {
            val conditionProps = collectPropertiesValues(mcVersionFilterParents)
            forgeVersions.sortedMcVersions.filter { version ->
                conditionProps["MC_VERSION"] = version
                TemplateEvaluator.condition(conditionProps, filterExpr).getOrDefault(true)
            }
        } else {
            forgeVersions.sortedMcVersions
        }

        mcVersionsModel.removeAllElements()
        mcVersionsModel.addAll(mcVersions)

        val selectedMcVersion = when {
            mcVersionProperty.get() in mcVersions -> mcVersionProperty.get()
            defaultValue.minecraft in mcVersions -> defaultValue.minecraft
            else -> mcVersions.first()
        }
        mcVersionProperty.set(selectedMcVersion)
    }

    companion object {
        private var hasDownloadedVersions = false

        private var forgeVersion: ForgeVersion? = null

        private fun downloadVersions(uiCallback: () -> Unit) {
            if (hasDownloadedVersions) {
                uiCallback()
                return
            }

            application.executeOnPooledThread {
                runBlocking {
                    forgeVersion = ForgeVersion.downloadData()

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
        ): CreatorProperty<*> = ForgeVersionsCreatorProperty(descriptor, graph, properties)
    }
}
