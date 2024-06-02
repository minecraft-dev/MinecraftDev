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

import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.model.NeoForgeVersions
import com.demonwav.mcdev.platform.neoforge.version.NeoForgeVersion
import com.demonwav.mcdev.platform.neoforge.version.NeoGradleVersion
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.util.application
import javax.swing.DefaultComboBoxModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class NeoForgeVersionsProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<NeoForgeVersions>(descriptor, graph, properties) {

    private val emptyVersion = SemanticVersion.release()

    private val defaultValue = createDefaultValue(descriptor.default)

    override val graphProperty: GraphProperty<NeoForgeVersions> = graph.property(defaultValue)
    var versions: NeoForgeVersions by graphProperty

    private var nfVersion: NeoForgeVersion? = null
    private var previousMcVersion: SemanticVersion? = null

    private val mcVersionProperty = graphProperty.transform({ it.minecraft }, { versions.copy(minecraft = it) })
    private val mcVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val nfVersionProperty = graphProperty.transform({ it.neoforge }, { versions.copy(neoforge = it) })
    private val nfVersionsModel = DefaultComboBoxModel<SemanticVersion>()
    private val ngVersionProperty = graphProperty.transform({ it.neogradle }, { versions.copy(neogradle = it) })
    private val ngVersionsModel = DefaultComboBoxModel<SemanticVersion>()

    override fun createDefaultValue(raw: Any?): NeoForgeVersions {
        if (raw is String) {
            return deserialize(raw)
        }

        return NeoForgeVersions(emptyVersion, emptyVersion, emptyVersion)
    }

    override fun serialize(value: NeoForgeVersions): String {
        return "${value.minecraft} ${value.neoforge} ${value.neogradle}"
    }

    override fun deserialize(string: String): NeoForgeVersions {
        val versions = string.split(' ')
            .take(3)
            .map { SemanticVersion.tryParse(it) ?: emptyVersion }

        return NeoForgeVersions(
            versions.getOrNull(0) ?: emptyVersion,
            versions.getOrNull(1) ?: emptyVersion,
            versions.getOrNull(2) ?: emptyVersion,
        )
    }

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            comboBox(mcVersionsModel)
                .bindItem(mcVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            comboBox(nfVersionsModel)
                .bindItem(nfVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            comboBox(ngVersionsModel)
                .bindItem(ngVersionProperty)
                .validationOnInput(BuiltinValidations.nonEmptyVersion)
                .validationOnApply(BuiltinValidations.nonEmptyVersion)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
    }

    override fun setupProperty() {
        super.setupProperty()

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

        application.executeOnPooledThread {
            runBlocking {
                val neoforgeVersions = NeoForgeVersion.downloadData()
                val neogradleVersions = NeoGradleVersion.downloadData()
                val mcVersions = neoforgeVersions?.sortedMcVersions?.let { mcVersion ->
                    val filterExpr = descriptor.parameters?.get("mcVersionFilter") as? String
                    if (filterExpr != null) {
                        mcVersion.filter { version ->
                            val conditionProps = mapOf("MC_VERSION" to version)
                            TemplateEvaluator.condition(conditionProps, filterExpr).getOrDefault(true)
                        }
                    } else {
                        mcVersion
                    }
                }

                if (neoforgeVersions != null && neogradleVersions != null && !mcVersions.isNullOrEmpty()) {
                    withContext(Dispatchers.Swing) {
                        nfVersion = neoforgeVersions

                        mcVersionsModel.removeAllElements()
                        mcVersionsModel.addAll(mcVersions)

                        val selectedMcVersion = when {
                            mcVersionProperty.get() in mcVersions -> mcVersionProperty.get()
                            defaultValue.minecraft in mcVersions -> defaultValue.minecraft
                            else -> mcVersions.first()
                        }
                        mcVersionProperty.set(selectedMcVersion)

                        val availableNgVersions = neogradleVersions.versions.take(descriptor.limit ?: 50)
                        ngVersionsModel.removeAllElements()
                        ngVersionsModel.addAll(availableNgVersions)
                        ngVersionProperty.set(availableNgVersions.firstOrNull() ?: emptyVersion)
                    }
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = NeoForgeVersionsProperty(descriptor, graph, properties)
    }
}
