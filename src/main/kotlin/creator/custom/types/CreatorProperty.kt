package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateEvaluator
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.getOrLogException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem

abstract class CreatorProperty<T>(
    val descriptor: TemplatePropertyDescriptor,
    val graph: PropertyGraph,
    protected val properties: Map<String, CreatorProperty<*>>
) {
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
    open fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): Any? {
        if (derivation.select != null) {
            return deriveSelectFirst(parentValues, derivation)
        }

        thisLogger().error("This type doesn't support derivation")
        return graphProperty.get()
    }

    fun deriveSelectFirst(parentValues: List<Any?>, derivation: PropertyDerivation): Any? {
        val properties = parentValues.mapIndexed { i, value -> derivation.parents!![i] to value }.toMap()
        for (select in derivation.select ?: emptyList()) {
            if (TemplateEvaluator.condition(properties, select.condition).getOrLogException(thisLogger()) == true) {
                return select.value
            }
        }

        return derivation.default
    }

    abstract fun buildUi(panel: Panel, context: WizardContext)

    open fun setupProperty() {
        if (descriptor.remember != false && descriptor.derives == null) {
            toStringProperty(graphProperty).bindStorage(makeStorageKey())
        }

        if (descriptor.derives != null) {
            val parents = descriptor.derives.parents
                ?: throw RuntimeException("No parents specified in derivation of property '${descriptor.name}'")
            for (parent in parents) {
                if (!properties.containsKey(parent)) {
                    throw RuntimeException("Unknown parent property '${parent}' in derivation of property '${descriptor.name}'")
                }
            }

            fun collectParentValues(): List<Any?> = parents.map { properties[it]!!.get() }

            @Suppress("UNCHECKED_CAST")
            graphProperty.set(derive(collectParentValues(), descriptor.derives) as T)
            for (parent in parents) {
                val parentProperty = properties[parent]!!
                graphProperty.dependsOn(parentProperty.graphProperty, descriptor.derives.whenModified != false) {
                    @Suppress("UNCHECKED_CAST")
                    derive(collectParentValues(), descriptor.derives) as T
                }
            }
        }

        if (descriptor.inheritFrom != null) {
            val parentProperty = properties[descriptor.inheritFrom]
                ?: throw RuntimeException("Unknown parent property '${descriptor.inheritFrom}' in derivation of property '${descriptor.name}'")

            @Suppress("UNCHECKED_CAST")
            graphProperty.set(parentProperty.graphProperty.get() as T)
            graphProperty.dependsOn(parentProperty.graphProperty, true) {
                @Suppress("UNCHECKED_CAST")
                parentProperty.graphProperty.get() as T
            }
        }
    }

    protected fun makeStorageKey(discriminator: String? = null): String {
        val base = "${javaClass.name}.property.${descriptor.name}.${descriptor.type}"
        if (discriminator == null) {
            return base
        }

        return "$base.$discriminator"
    }

    protected fun <E> Panel.buildDropdownUi(options: List<E>, graphProp: GraphProperty<E>) {
        row(descriptor.label) {
            comboBox(options)
                .bindItem(graphProp)
                .enabled(descriptor.editable != false)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.visible(descriptor.hidden != true)
    }
}
