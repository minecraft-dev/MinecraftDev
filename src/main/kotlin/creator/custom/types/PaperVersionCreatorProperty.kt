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

import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.McdevHttp
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.fetchOrNull
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

open class PaperVersionCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    context: CreatorContext
) : SemanticVersionCreatorProperty(descriptor, context) {

    companion object {
        private val LOGGER = logger<PaperVersionCreatorProperty>()
        private const val URL = "https://fill.papermc.io/v3/projects/paper"

        @Volatile
        private var paperVersions: List<SemanticVersion>? = null

        internal fun clearCache() {
            paperVersions = null
        }

        /**
         * Returns the Paper release versions from 1.18.2 on, newest first, or null after logging a warning if the
         * download fails or finds none.
         */
        suspend fun getPaperVersions(): List<SemanticVersion>? {
            paperVersions?.let { return it }

            val versions = fetchOrNull(LOGGER, URL) {
                val body = McdevHttp.get(URL) {
                    userAgent("minecraft-dev/${PluginUtil.pluginVersion} (https://github.com/minecraft-dev/MinecraftDev)")
                }.suspend().connect { readString() }
                parse(body)
            } ?: return null
            if (versions.isEmpty()) {
                LOGGER.warn("No Paper versions found at $URL")
                return null
            }

            paperVersions = versions
            return versions
        }

        private fun parse(body: String): List<SemanticVersion> {
            val groups = Json.parseToJsonElement(body).jsonObject["versions"]?.jsonObject
                ?: throw IOException("Unexpected Paper version data")
            return groups.values.asSequence()
                .flatMap { it.jsonArray }
                .map { it.jsonPrimitive.content }
                .mapNotNull { SemanticVersion.tryParse(it) }
                // only release versions
                .filter { ver -> ver.parts.all { it.isReleasePart } }
                // nothing lower than 1.18.2 should be selectable
                .filter { it >= MinecraftVersions.MC1_18_2 }
                .sortedDescending()
                .toList()
        }
    }

    internal val downloadStatus = VersionDownloadStatus(graph)
    private val versionsProperty = graph.property<Set<SemanticVersion>>(emptySet())

    override fun buildUi(panel: Panel) {
        panel.row(descriptor.translatedLabel) {
            val combobox = comboBox(versionsProperty.get())
                .bindItem(graphProperty)
                .enabled(descriptor.editable != false)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            downloadStatus(downloadStatus, makeStorageKey("progress"))

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
        val scope = context.childScope("PaperVersionCreatorProperty")
        scope.launch(Dispatchers.Default) {
            val result = getPaperVersions()
            withContext(context.uiContext) {
                if (result == null) {
                    downloadStatus.failed()
                } else {
                    versionsProperty.set(result.toSet())
                    downloadStatus.succeeded()
                }
            }
        }
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            context: CreatorContext
        ): CreatorProperty<*> = PaperVersionCreatorProperty(descriptor, context)
    }
}
