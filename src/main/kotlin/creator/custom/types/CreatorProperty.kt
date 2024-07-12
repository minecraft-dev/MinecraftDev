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

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.derivation.PreparedDerivation
import com.demonwav.mcdev.creator.custom.derivation.SelectPropertyDerivation
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row

abstract class CreatorProperty<T>(
    val descriptor: TemplatePropertyDescriptor,
    val graph: PropertyGraph,
    protected val properties: Map<String, CreatorProperty<*>>,
    val valueType: Class<T>
) {
    private var derivation: PreparedDerivation? = null
    private lateinit var visibleProperty: GraphProperty<Boolean>

    abstract val graphProperty: GraphProperty<T>

    abstract fun createDefaultValue(raw: Any?): T

    abstract fun serialize(value: T): String

    abstract fun deserialize(string: String): T

    open fun toStringProperty(graphProperty: GraphProperty<T>): ObservableMutableProperty<String> =
        graphProperty.transform(::serialize, ::deserialize)

    open fun get(): T? {
        val value = graphProperty.get()
        if (descriptor.nullIfDefault == true) {
            val default = createDefaultValue(descriptor.default)
            if (value == default) {
                return null
            }
        }

        return value
    }

    fun acceptsType(type: Class<*>): Boolean = type.isAssignableFrom(valueType)

    /**
     * Produces a new value based on the provided [parentValues] and the template-defined [derivation] configuration.
     *
     * You must **NOT** [set][GraphProperty.set] the value of [graphProperty] in the process. You may however [get][GraphProperty.get] it at will.
     *
     * @param parentValues the values of the properties this [graphProperty] depends on
     * @param derivation the configuration of the desired derivation
     *
     * @see GraphProperty.dependsOn
     */
    open fun derive(parentValues: List<Any?>?, derivation: PropertyDerivation): Any? {
        if (this.derivation == null) {
            throw IllegalStateException("This property has not been configured with a derivation")
        }

        val result = this.derivation!!.derive(parentValues.orEmpty())
        if (this.derivation is SelectPropertyDerivation) {
            return convertSelectDerivationResult(result)
        }

        return result
    }

    protected open fun convertSelectDerivationResult(original: Any?): Any? = original

    abstract fun buildUi(panel: Panel, context: WizardContext)

    /**
     * Prepares everything this property needs, like calling [GraphProperty]'s [GraphProperty.afterChange] and
     * [GraphProperty.dependsOn] on this property or other properties declared before this one.
     *
     * [properties] contains all the properties declared in the descriptor
     * up to this one, forward references are not permitted.
     *
     * This is also where you should validate the [descriptor] values you want to use, and report all validation errors
     * or warnings through the [reporter], use [TemplateValidationReporter.fatal] if the error is a show-stopper and
     * the validation cannot even proceed further.
     */
    open fun setupProperty(reporter: TemplateValidationReporter) {
        if (descriptor.remember != false && descriptor.derives == null) {
            val storageKey = when (val remember = descriptor.remember) {
                null, true -> makeStorageKey()
                is String -> makeCustomStorageKey(remember)
                else -> {
                    reporter.error("Invalid 'remember' value. Must be a boolean or a string")
                    null
                }
            }

            if (storageKey != null) {
                toStringProperty(graphProperty).bindStorage(storageKey)
            }
        }

        visibleProperty = setupVisibleProperty(reporter, descriptor.visible)

        if (descriptor.derives != null) {
            val parents = descriptor.derives.parents
                ?: return reporter.error("No parents specified in derivation")
            for (parent in parents) {
                if (!properties.containsKey(parent)) {
                    return reporter.error("Unknown parent property '$parent' in derivation")
                }
            }

            derivation = setupDerivation(reporter, descriptor.derives)
            if (derivation == null) {
                reporter.fatal("Unknown method derivation: ${descriptor.derives}")
            }

            @Suppress("UNCHECKED_CAST")
            graphProperty.set(derive(collectDerivationParentValues(reporter), descriptor.derives) as T)
            for (parent in parents) {
                val parentProperty = properties[parent]!!
                graphProperty.dependsOn(parentProperty.graphProperty, descriptor.derives.whenModified != false) {
                    @Suppress("UNCHECKED_CAST")
                    derive(collectDerivationParentValues(), descriptor.derives) as T
                }
            }
        }

        if (descriptor.inheritFrom != null) {
            val parentProperty = properties[descriptor.inheritFrom]
                ?: return reporter.error("Unknown parent property '${descriptor.inheritFrom}' in derivation")

            @Suppress("UNCHECKED_CAST")
            graphProperty.set(parentProperty.graphProperty.get() as T)
            graphProperty.dependsOn(parentProperty.graphProperty, true) {
                @Suppress("UNCHECKED_CAST")
                parentProperty.graphProperty.get() as T
            }
        }
    }

    protected open fun setupDerivation(
        reporter: TemplateValidationReporter,
        derives: PropertyDerivation
    ): PreparedDerivation? = null

    protected fun makeStorageKey(discriminator: String? = null): String {
        val base = "${javaClass.name}.property.${descriptor.name}.${descriptor.type}"
        if (discriminator == null) {
            return base
        }

        return "$base.$discriminator"
    }

    protected fun makeCustomStorageKey(key: String): String {
        return "${javaClass.name}.property.$key"
    }

    protected fun collectPropertiesValues(names: List<String>? = null): MutableMap<String, Any?> {
        val into = mutableMapOf<String, Any?>()

        into.putAll(TemplateEvaluator.baseProperties)

        return if (names == null) {
            properties.mapValuesTo(into) { (_, prop) -> prop.get() }
        } else {
            names.associateWithTo(mutableMapOf()) { properties[it]?.get() }
        }
    }

    protected fun collectDerivationParents(reporter: TemplateValidationReporter? = null): List<CreatorProperty<*>?>? =
        descriptor.derives?.parents?.map { parentName ->
            val property = properties[parentName]
            if (property == null) {
                reporter?.error("Unknown parent property: $parentName")
            }
            return@map property
        }

    protected fun collectDerivationParentValues(reporter: TemplateValidationReporter? = null): List<Any?>? =
        descriptor.derives?.parents?.map { parentName ->
            val property = properties[parentName]
            if (property == null) {
                reporter?.error("Unknown parent property: $parentName")
            }
            return@map property?.get()
        }

    protected fun Row.propertyVisibility(): Row = this.visibleIf(visibleProperty)

    private fun setupVisibleProperty(
        reporter: TemplateValidationReporter,
        visibility: Any?
    ): GraphProperty<Boolean> {
        val prop = graph.property(true)
        if (visibility == null || visibility is Boolean) {
            prop.set(visibility != false)
            return prop
        }

        if (visibility !is Map<*, *>) {
            reporter.error("Visibility can only be a boolean or an object")
            return prop
        }

        var dependsOn = visibility["dependsOn"]
        if (dependsOn !is String && (dependsOn !is List<*> || dependsOn.any { it !is String })) {
            reporter.error(
                "Expected 'visible' to have a 'dependsOn' value that is either a string or a list of strings"
            )
            return prop
        }

        val dependenciesNames = when (dependsOn) {
            is String -> setOf(dependsOn)
            is Collection<*> -> dependsOn.filterIsInstance<String>().toSet()
            else -> throw IllegalStateException("Should not be reached")
        }
        val dependencies = dependenciesNames.mapNotNull {
            val dependency = this.properties[it]
            if (dependency == null) {
                reporter.error("Visibility dependency '$it' does not exist")
            }
            dependency
        }
        if (dependencies.size != dependenciesNames.size) {
            // Errors have already been reported
            return prop
        }

        val condition = visibility["condition"]
        if (condition !is String) {
            reporter.error("Expected 'visible' to have a 'condition' string")
            return prop
        }

        var didInitialUpdate = false
        val update: () -> Boolean = {
            val conditionProperties = dependencies.associate { prop -> prop.descriptor.name to prop.get() }
            val result = TemplateEvaluator.condition(conditionProperties, condition)
            val exception = result.exceptionOrNull()
            if (exception != null) {
                if (!didInitialUpdate) {
                    didInitialUpdate = true
                    reporter.error("Failed to compute initial visibility: ${exception.message}")
                    thisLogger().info("Failed to compute initial visibility: ${exception.message}", exception)
                } else {
                    thisLogger().error("Failed to compute initial visibility: ${exception.message}", exception)
                }
            }

            result.getOrDefault(true)
        }

        prop.set(update())
        for (dependency in dependencies) {
            prop.dependsOn(dependency.graphProperty, deleteWhenModified = false, update)
        }

        return prop
    }
}
