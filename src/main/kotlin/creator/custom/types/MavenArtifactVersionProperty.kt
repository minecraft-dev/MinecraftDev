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

class MavenArtifactVersionProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SemanticVersionCreatorProperty(graph, descriptor, properties) {

    val sourceUrl: String
        get() = descriptor.parameters!!["sourceUrl"] as String

    override val graphProperty: GraphProperty<SemanticVersion> = graph.property(SemanticVersion(emptyList()))
    private val versionsProperty = graph.property<Collection<SemanticVersion>>(emptyList())
    private val loadingVersionsProperty = graph.property(true)

    init {
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

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
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
        }.visible(descriptor.hidden != true)
    }

    class Factory : CreatorPropertyFactory {

        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = MavenArtifactVersionProperty(graph, descriptor, properties)
    }
}
