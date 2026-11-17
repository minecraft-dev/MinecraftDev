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
import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.CreatorCredentials
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.getOrLogException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.ui.AsyncProcessIcon
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MavenArtifactVersionCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    context: CreatorContext
) : SemanticVersionCreatorProperty(descriptor, context) {

    lateinit var sourceUrl: String
    var rawVersionFilter: (String) -> Boolean = { true }
    var versionFilter: (SemanticVersion) -> Boolean = { true }

    override val graphProperty: GraphProperty<SemanticVersion> = graph.property(SemanticVersion(emptyList()))
    private val versionsProperty = graph.property<Set<SemanticVersion>>(emptySet())
    private val loadingVersionsProperty = graph.property(true)
    private val loadingVersionsStatusProperty = graph.property("")

    override fun buildUi(panel: Panel) {
        panel.row(descriptor.translatedLabel) {
            val combobox = comboBox(versionsProperty.get())
                .bindItem(graphProperty)
                .enabled(descriptor.editable != false)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            cell(AsyncProcessIcon(makeStorageKey("progress")))
                .visibleIf(loadingVersionsProperty)
            label("").applyToComponent { foreground = JBColor.RED }
                .bindText(loadingVersionsStatusProperty)
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
            reporter.error(MCDevBundle("creator.template.validation.expected_source_url"))
            return
        }

        sourceUrl = url

        val rawVersionFilterCondition = descriptor.parameters?.get("rawVersionFilter")
        if (rawVersionFilterCondition != null) {
            if (rawVersionFilterCondition !is String) {
                reporter.error(MCDevBundle("creator.template.validation.must_be_string", "rawVersionFilter"))
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
                reporter.error(MCDevBundle("creator.template.validation.must_be_string", "versionFilter"))
            } else {
                versionFilter = { version ->
                    val props = mapOf("version" to version)
                    TemplateEvaluator.condition(props, versionFilterCondition)
                        .getOrLogException(thisLogger()) == true
                }
            }
        }

        downloadVersions(
            context,
            // The key might be a bit too unique, but that'll do the job
            descriptor.name + "@" + descriptor.hashCode(),
            sourceUrl,
            rawVersionFilter,
            versionFilter,
            descriptor.limit ?: 50
        ) { result ->
            result.onSuccess { versions ->
                versionsProperty.set(versions.toSet())
                loadingVersionsProperty.set(false)
            }.onFailure { exception ->
                loadingVersionsStatusProperty.set(exception.message ?: exception.javaClass.simpleName)
            }
        }
    }

    companion object {

        private val versionsCache = ConcurrentHashMap<String, List<SemanticVersion>>()

        internal fun clearCache() {
            versionsCache.clear()
        }

        private fun downloadVersions(
            context: CreatorContext,
            key: String,
            url: String,
            rawVersionFilter: (String) -> Boolean,
            versionFilter: (SemanticVersion) -> Boolean,
            limit: Int,
            uiCallback: (Result<List<SemanticVersion>>) -> Unit
        ) {
            // Let's not mix up cached versions if different properties
            // point to the same URL, but have different filters or limits
            val cacheKey = "$key-$url"
            val cachedVersions = versionsCache[cacheKey]
            if (cachedVersions != null) {
                uiCallback(Result.success(cachedVersions))
                return
            }

            val scope = context.childScope("MavenArtifactVersionCreatorProperty")
            scope.launch(Dispatchers.Default) {
                val result = withContext(Dispatchers.IO) {
                    val headers = CreatorCredentials.findMavenRepoAuthHeaders(url)
                    runCatching { collectMavenVersions(url, headers) }
                }.map { result ->
                    val versions = result.asSequence()
                        .filter(rawVersionFilter)
                        .mapNotNull(SemanticVersion::tryParse)
                        .filter(versionFilter)
                        .sortedDescending()
                        .take(limit)
                        .toList()

                    versionsCache[cacheKey] = versions
                    versions
                }

                withContext(context.uiContext) {
                    uiCallback(result)
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {

        override fun create(
            descriptor: TemplatePropertyDescriptor,
            context: CreatorContext
        ): CreatorProperty<*> = MavenArtifactVersionCreatorProperty(descriptor, context)
    }
}
