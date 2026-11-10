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

import com.demonwav.mcdev.creator.collectMavenVersions
import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.CreatorCredentials
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.TemplateApi
import com.demonwav.mcdev.creator.custom.types.GradlePluginSelectorCreatorProperty.Holder
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.getOrLogException
import com.demonwav.mcdev.util.toBooleanLenient
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.extensions.authentication
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.ui.AsyncProcessIcon
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GradlePluginSelectorCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    context: CreatorContext
) : CreatorProperty<Holder>(descriptor, context, Holder::class.java) {
    private val loadingVersionsProperty = graph.property(true)
    private val loadingVersionsStatusProperty = graph.property("")

    private val defaultValue = createDefaultValue(descriptor.default)

    override val graphProperty: GraphProperty<Holder> = graph.property(defaultValue)
    var property: Holder by graphProperty

    private val versionsModel = graph.property<Set<SemanticVersion>>(emptySet())

    private val versionProperty = graphProperty.transform({ it.version }, { property.copy(version = it) })
    private val enabledProperty = graphProperty.transform({ it.enabled }, { property.copy(enabled = it) })

    override fun createDefaultValue(raw: Any?): Holder {
        if (raw is Boolean) {
            return Holder(SemanticVersion(emptyList()), raw)
        }
        if (raw is String) {
            return deserialize(raw)
        }

        return Holder(SemanticVersion(emptyList()), false)
    }

    override fun serialize(value: Holder): String = value.toString()

    override fun deserialize(string: String): Holder =
        Holder.tryParse(string) ?: Holder(SemanticVersion(emptyList()), false)

    override fun handleForceValueProperty(
        original: Holder,
        string: String
    ): Holder = original.copy(enabled = string.toBooleanStrictOrNull() ?: original.enabled)

    override fun buildUi(panel: Panel) {
        val label = descriptor.translatedLabel
        panel.row(label) {
            val checkbox = checkBox("")
                .bindSelected(enabledProperty)
                .enabled(descriptor.editable != false)

            val checkboxUpdater = createCheckboxEnabledToggleMethod(enabledProperty, checkbox)
            checkboxUpdater.update(forceValueProperty?.get())
            forceValueProperty?.afterChange(checkboxUpdater::update)

            label("Version:").gap(RightGap.SMALL)
            val combobox = comboBox(versionsModel.get())
                .bindItem(versionProperty)
                .enabled(descriptor.editable != false)
                .also { ComboboxSpeedSearch.installOn(it.component) }

            val warning = descriptor.translatedWarning
            if (warning != null) {
                combobox.comment(descriptor.translate(warning))
            }

            cell(AsyncProcessIcon(makeStorageKey("progress")))
                .visibleIf(loadingVersionsProperty)
            label("").applyToComponent { foreground = JBColor.RED }
                .bindText(loadingVersionsStatusProperty)
                .visibleIf(loadingVersionsProperty)

            versionsModel.afterChange { versions ->
                combobox.component.removeAllItems()
                for (version in versions) {
                    combobox.component.addItem(version)
                }
            }
        }.propertyVisibility()
    }

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        var rawVersionFilter: (String) -> Boolean = { true }
        var versionFilter: (SemanticVersion) -> Boolean = { true }

        val url = descriptor.parameters?.get("sourceUrl") as? String
        if (url == null) {
            reporter.error("Expected string parameter 'sourceUrl'")
            return
        }

        val rawVersionFilterCondition = descriptor.parameters["rawVersionFilter"]
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

        val versionFilterCondition = descriptor.parameters["versionFilter"]
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
            context,
            // The key might be a bit too unique, but that'll do the job
            descriptor.name + "@" + descriptor.hashCode(),
            url,
            rawVersionFilter,
            versionFilter,
            descriptor.limit ?: 50
        ) { result ->
            result.onSuccess { versions ->
                val set = versions.toSet()
                versionsModel.set(set)
                loadingVersionsProperty.set(false)
            }.onFailure { exception ->
                loadingVersionsStatusProperty.set(exception.message ?: exception.javaClass.simpleName)
            }
        }
    }

    companion object {

        private var versionsCache = ConcurrentHashMap<String, List<SemanticVersion>>()

        fun downloadVersions(
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

            val scope = context.childScope("GradlePluginSelectorCreatorProperty")
            scope.launch(Dispatchers.Default) {
                val result = withContext(Dispatchers.IO) {
                    val requestCustomizer = CreatorCredentials.findMavenRepoCredentials(url)?.let { (user, pass) ->
                        Function<Request, Request> { request -> request.authentication().basic(user, pass) }
                    }

                    runCatching { collectMavenVersions(url, requestCustomizer) }
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
            context: CreatorContext,
            reporter: TemplateValidationReporter
        ): CreatorProperty<*> =
            GradlePluginSelectorCreatorProperty(descriptor, context).initForceValueProperty(reporter)
    }

    @TemplateApi
    data class Holder(
        val version: SemanticVersion,
        val enabled: Boolean
    ) {
        override fun toString(): String {
            return "$enabled $version"
        }

        companion object {
            fun tryParse(raw: String): Holder? {
                val split = raw.split(" ", limit = 2)
                return when (split.size) {
                    1 -> raw.toBooleanLenient()?.let { Holder(SemanticVersion(emptyList()), it) }
                    2 -> split[0].toBooleanLenient()?.let {
                        Holder(
                            SemanticVersion.tryParse(split[1]) ?: SemanticVersion(emptyList()),
                            it
                        )
                    }

                    else -> null
                }
            }
        }
    }
}
