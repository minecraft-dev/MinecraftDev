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
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.util.application
import com.intellij.util.ui.AsyncProcessIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class MavenArtifactVersionCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SemanticVersionCreatorProperty(descriptor, graph, properties) {

    lateinit var sourceUrl: String

    override val graphProperty: GraphProperty<SemanticVersion> = graph.property(SemanticVersion(emptyList()))
    private val versionsProperty = graph.property<Collection<SemanticVersion>>(emptyList())
    private val loadingVersionsProperty = graph.property(true)

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            val combobox = comboBox(versionsProperty.get())
                .bindItem(graphProperty)
                .enabled(descriptor.editable != false)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            cell(AsyncProcessIcon(makeStorageKey("progress")))
                .visibleIf(loadingVersionsProperty)

            versionsProperty.afterChange { versions ->
                combobox.component.removeAllItems()
                for (version in versions) {
                    combobox.component.addItem(version)
                }
            }
        }.propertyVisibility()
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        val url = descriptor.parameters?.get("sourceUrl") as? String
        if (url == null) {
            reporter.error("Expected string parameter 'sourceUrl'")
            return
        }

        sourceUrl = url

        application.executeOnPooledThread {
            runBlocking {
                val versions = collectMavenVersions(sourceUrl)
                    .asSequence()
                    .mapNotNull(SemanticVersion::tryParse)
                    .sortedDescending()
                    .take(descriptor.limit ?: 50)
                    .toList()
                withContext(Dispatchers.Swing) {
                    versionsProperty.set(versions)
                    loadingVersionsProperty.set(false)
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {

        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = MavenArtifactVersionCreatorProperty(descriptor, graph, properties)
    }
}
