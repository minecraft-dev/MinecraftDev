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
import com.demonwav.mcdev.creator.custom.CreatorContext
import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.derivation.PreparedDerivation
import com.demonwav.mcdev.creator.custom.derivation.SelectPropertyDerivation
import com.demonwav.mcdev.creator.custom.derivation.UnknownDerivation
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row

abstract class CreatorProperty<T>(
    val descriptor: TemplatePropertyDescriptor,
    protected val context: CreatorContext,
    val valueType: Class<T>
) {
    protected val graph: PropertyGraph
        get() = context.graph

    protected val properties
        get() = context.properties

    protected val wizardContext: WizardContext
        get() = context.wizardContext

    private var derivation: PreparedDerivation? = null
    private lateinit var visibleProperty: GraphProperty<Boolean>

    var forceValueProperty: GraphProperty<String?>? = null

    abstract val graphProperty: GraphProperty<T>

    abstract fun createDefaultValue(raw: Any?): T

    abstract fun serialize(value: T): String

    abstract fun deserialize(string: String): T

    open fun handleForceValueProperty(original: T, string: String): T = original

    open fun toStringProperty(graphProperty: GraphProperty<T>): ObservableMutableProperty<String> =
        graphProperty.transform(::serialize, ::deserialize)

    open fun get(): T? {
        val forcedValue = forceValueProperty?.get()
        var value = graphProperty.get()
        if (forcedValue != null) {
            value = handleForceValueProperty(value, forcedValue)
        }

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

    abstract fun buildUi(panel: Panel)

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
                    reporter.error(MCDevBundle("creator.template.validation.invalid_remember"))
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
                ?: return reporter.error(MCDevBundle("creator.template.validation.no_derivation_parents"))
            for (parent in parents) {
                if (!properties.containsKey(parent)) {
                    return reporter.error(MCDevBundle("creator.template.validation.unknown_derivation_parent", parent))
                }
            }

            derivation = setupDerivation(reporter, descriptor.derives)
            if (derivation == UnknownDerivation) {
                reporter.fatal("Unknown method derivation: ${descriptor.derives}")
            } else if (derivation != null) {
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
        }

        if (descriptor.inheritFrom != null) {
            val parentProperty = properties[descriptor.inheritFrom]
                ?: return reporter.error(
                    MCDevBundle("creator.template.validation.unknown_derivation_parent", descriptor.inheritFrom),
                )

            @Suppress("UNCHECKED_CAST")
            graphProperty.set(parentProperty.graphProperty.get() as T)
            graphProperty.dependsOn(parentProperty.graphProperty, true) {
                @Suppress("UNCHECKED_CAST")
                parentProperty.graphProperty.get() as T
            }
        }
    }

    /**
     * @return [UnknownDerivation] if the derivation method is unknown, `null` if the derivation is invalid.
     */
    protected open fun setupDerivation(
        reporter: TemplateValidationReporter,
        derives: PropertyDerivation
    ): PreparedDerivation? = UnknownDerivation

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

        return names?.associateWithTo(mutableMapOf()) { properties[it]?.get() }
            ?: properties.mapValuesTo(into) { (_, prop) -> prop.get() }
    }

    protected fun collectDerivationParents(reporter: TemplateValidationReporter? = null): List<CreatorProperty<*>?>? =
        descriptor.derives?.parents?.map { parentName ->
            val property = properties[parentName]
            if (property == null) {
                reporter?.error(MCDevBundle("creator.template.validation.unknown_parent", parentName))
            }
            return@map property
        }

    protected fun collectDerivationParentValues(reporter: TemplateValidationReporter? = null): List<Any?>? =
        descriptor.derives?.parents?.map { parentName ->
            val property = properties[parentName]
            if (property == null) {
                reporter?.error(MCDevBundle("creator.template.validation.unknown_parent", parentName))
            }
            return@map property?.get()
        }

    protected fun Row.propertyVisibility(): Row = this.visibleIf(visibleProperty)

    fun setupVisibleProperty(
        reporter: TemplateValidationReporter,
        visibility: Any?
    ): GraphProperty<Boolean> {
        return setupVisibleProperty(graph, properties, reporter, visibility)
    }

    fun initForceValueProperty(reporter: TemplateValidationReporter): CreatorProperty<T> {
        this.forceValueProperty = setupForceValueProperty(graph, properties, reporter, descriptor.forceValue)
        return this
    }

    fun createCheckboxEnabledToggleMethod(
        property: ObservableMutableProperty<Boolean>,
        checkbox: Cell<JBCheckBox>
    ): CheckboxUpdater {
        class Inner : CheckboxUpdater {
            private var previousEnabledStatus = property.get()
            override fun update(str: String?) {
                if (str == null) {
                    checkbox.enabled(descriptor.editable != false)
                    property.set(previousEnabledStatus)
                } else {
                    str.toBooleanStrictOrNull()?.let {
                        checkbox.enabled(false)
                        previousEnabledStatus = property.get()
                        property.set(it)
                    }
                }
            }
        }

        return Inner()
    }

    companion object {
        private fun obtainDependencies(
            properties: Map<String, CreatorProperty<*>>,
            reporter: TemplateValidationReporter,
            property: Map<*, *>,
            name: String
        ): List<CreatorProperty<*>>? {
            val dependsOn = property["dependsOn"]
            if (dependsOn !is String && (dependsOn !is List<*> || dependsOn.any { it !is String })) {
                reporter.error(
                    MCDevBundle("creator.template.validation.invalid_depends_on", name)
                )
                return null
            }

            val dependenciesNames = when (dependsOn) {
                is String -> setOf(dependsOn)
                is Collection<*> -> dependsOn.filterIsInstance<String>().toSet()
                else -> throw IllegalStateException("Should not be reached")
            }
            val dependencies = dependenciesNames.mapNotNull {
                val dependency = properties[it]
                if (dependency == null) {
                    reporter.error(MCDevBundle("creator.template.validation.unknown_dependency", name, it))
                }
                dependency
            }
            if (dependencies.size != dependenciesNames.size) {
                // Errors have already been reported
                return null
            }

            return dependencies
        }

        private fun setupDependableProperty(
            dependencies: List<CreatorProperty<*>>,
            reporter: TemplateValidationReporter,
            property: Map<*, *>,
            name: String,
            prop: GraphProperty<Boolean>
        ): GraphProperty<Boolean> {
            val condition = property["condition"]
            if (condition !is String) {
                reporter.error(MCDevBundle("creator.template.validation.missing_condition", name))
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
                        reporter.error(
                            MCDevBundle("creator.template.validation.initial_value_failed", name, exception.message),
                        )
                        thisLogger().info("Failed to compute initial $name-property: ${exception.message}", exception)
                    } else {
                        thisLogger().error("Failed to compute initial $name-property: ${exception.message}", exception)
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

        fun setupVisibleProperty(
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>,
            reporter: TemplateValidationReporter,
            visibility: Any?
        ): GraphProperty<Boolean> {
            val prop = graph.property(true)
            if (visibility == null || visibility is Boolean) {
                prop.set(visibility != false)
                return prop
            }

            if (visibility !is Map<*, *>) {
                reporter.error(MCDevBundle("creator.template.validation.invalid_visibility"))
                return prop
            }

            val dependencies = obtainDependencies(properties, reporter, visibility, "visible")
                ?: return prop // The error has already been reported

            return setupDependableProperty(dependencies, reporter, visibility, "visible", prop)
        }

        fun setupForceValueProperty(
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>,
            reporter: TemplateValidationReporter,
            forceValue: Any?
        ): GraphProperty<String?> {
            val out = graph.property<String?>(null)

            if (forceValue == null) {
                return out
            }

            if (forceValue !is Map<*, *>) {
                reporter.error(MCDevBundle("creator.template.validation.invalid_force_value"))
                return out
            }

            val dependencies = obtainDependencies(properties, reporter, forceValue, "forceValue")
                ?: return out // The error has already been reported

            val value = forceValue["value"]
            if (value !is String) {
                reporter.error(MCDevBundle("creator.template.validation.missing_force_value"))
                return out
            }

            val conditionProperty =
                setupDependableProperty(dependencies, reporter, forceValue, "forceValue", graph.property(false))

            val update: () -> String? = {
                if (conditionProperty.get()) {
                    val conditionProperties = dependencies.associate { prop -> prop.descriptor.name to prop.get() }
                    val result = TemplateEvaluator.template(conditionProperties, value)
                    val exception = result.exceptionOrNull()
                    if (exception != null) {
                        thisLogger().error("Failed to compute forceValue-property: ${exception.message}", exception)
                    }

                    result.getOrNull()
                } else {
                    null
                }
            }

            out.set(update())
            for (dependency in dependencies) {
                out.dependsOn(dependency.graphProperty, deleteWhenModified = false, update)
            }

            return out
        }
    }

    @FunctionalInterface
    interface CheckboxUpdater {
        fun update(str: String?)
    }
}
