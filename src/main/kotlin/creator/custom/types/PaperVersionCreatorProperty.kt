/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.ui.AsyncProcessIcon
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

open class PaperVersionCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    context: CreatorContext
) : SemanticVersionCreatorProperty(descriptor, context) {

    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        private var paperVersions: List<SemanticVersion>? = null

        suspend fun getPaperVersions(): List<SemanticVersion> {
            paperVersions?.let { return it }

            val client = HttpClient()
            val response = client.request("https://fill.papermc.io/v3/projects/paper", block = {
                this.header("User-Agent", "minecraft-dev/${PluginUtil.pluginVersion} (https://github.com/minecraft-dev/MinecraftDev)")
            })
            if (response.status.isSuccess()) {
                val element = Json.parseToJsonElement(response.bodyAsText())
                val result = element.jsonObject["versions"]?.jsonObject?.values
                    ?.asSequence()
                    ?.flatMap { it.jsonArray }
                    ?.map { it.jsonPrimitive.content }
                    ?.mapNotNull { SemanticVersion.tryParse(it) }
                    // only release versions
                    ?.filter { ver -> ver.parts.all { it is SemanticVersion.Companion.VersionPart.ReleasePart } }
                    // nothing lower than 1.18.2 should be selectable
                    ?.filter { it >= MinecraftVersions.MC1_18_2 }
                    ?.toList()
                    ?.sortedDescending()

                if (result != null) {
                    paperVersions = result
                    return result
                }
            }

            return emptyList()
        }
    }

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
        val scope = context.childScope("PaperVersionCreatorProperty")
        scope.launch(Dispatchers.Default) {
            val result = getPaperVersions()
            versionsProperty.set(result.toSet())
            loadingVersionsProperty.set(false)
        }
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            context: CreatorContext
        ): CreatorProperty<*> = PaperVersionCreatorProperty(descriptor, context)
    }
}
