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
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.util.application
import com.intellij.util.ui.AsyncProcessIcon
import java.util.concurrent.ConcurrentHashMap
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
    var rawVersionFilter: (String) -> Boolean = { true }
    var versionFilter: (SemanticVersion) -> Boolean = { true }

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

        val rawVersionFilterCondition = descriptor.parameters?.get("rawVersionFilter")
        if (rawVersionFilterCondition != null) {
            if (rawVersionFilterCondition !is String) {
                reporter.error("'rawVersionFilter' must be a string")
            } else {
                rawVersionFilter = { version ->
                    val props = mapOf("version" to version)
                    TemplateEvaluator.condition(props, rawVersionFilterCondition)
                        .getOrLogException(thisLogger()) == true
                }
            }
        }

        val versionFilterCondition = descriptor.parameters?.get("versionFilter")
        if (versionFilterCondition != null) {
            if (versionFilterCondition !is String) {
                reporter.error("'versionFilter' must be a string")
            } else {
                versionFilter = { version ->
                    val props = mapOf("version" to version)
                    TemplateEvaluator.condition(props, versionFilterCondition)
                        .getOrLogException(thisLogger()) == true
                }
            }
        }

        downloadVersions(
            // The key might be a bit too unique, but that'll do the job
            descriptor.name + "@" + descriptor.hashCode(),
            sourceUrl,
            rawVersionFilter,
            versionFilter,
            descriptor.limit ?: 50
        ) { versions ->
            versionsProperty.set(versions)
            loadingVersionsProperty.set(false)
        }
    }

    companion object {

        private var versionsCache = ConcurrentHashMap<String, List<SemanticVersion>>()

        private fun downloadVersions(
            key: String,
            url: String,
            rawVersionFilter: (String) -> Boolean,
            versionFilter: (SemanticVersion) -> Boolean,
            limit: Int,
            uiCallback: (List<SemanticVersion>) -> Unit
        ) {
            // Let's not mix up cached versions if different properties
            // point to the same URL, but have different filters or limits
            val cacheKey = "$key-$url"
            val cachedVersions = versionsCache[cacheKey]
            if (cachedVersions != null) {
                uiCallback(cachedVersions)
                return
            }

            application.executeOnPooledThread {
                runBlocking {
                    val versions = collectMavenVersions(url)
                        .asSequence()
                        .filter(rawVersionFilter)
                        .mapNotNull(SemanticVersion::tryParse)
                        .filter(versionFilter)
                        .sortedDescending()
                        .take(limit)
                        .toList()

                    versionsCache[cacheKey] = versions

                    withContext(Dispatchers.Swing) {
                        uiCallback(versions)
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
        ): CreatorProperty<*> = MavenArtifactVersionCreatorProperty(descriptor, graph, properties)
    }
}
