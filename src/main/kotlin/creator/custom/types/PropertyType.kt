package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.dsl.builder.Panel

interface PropertyType<T> {

    fun createDefaultValue(raw: Any?): T

    fun serialize(value: T): String

    fun deserialize(string: String): T

    fun toStringProperty(graphProperty: GraphProperty<T>): ObservableMutableProperty<String> =
        graphProperty.transform(::serialize, ::deserialize)

    /**
     * Produces a new value based on the provided [parentValues] property value and the template-defined [derivation] configuration.
     *
     * You must **NOT** [set][GraphProperty.set] the value of [property] in the process. You may however [get][GraphProperty.get] it at will.
     *
     * @param property the property depending on [parentValues]
     * @param parentValues the GraphProperty and PropertyType this [property] depends on
     * @param derivation the configuration of the desired derivation
     *
     * @see GraphProperty.dependsOn
     */
    fun derive(
        property: GraphProperty<T>,
        parentValues: List<Any?>,
        properties: Map<String, Any?>,
        derivation: PropertyDerivation
    ): Any? {
        thisLogger().error("This type doesn't support derivation")
        return property.get()
    }

    fun Panel.buildUi(context: WizardContext, graphProperty: GraphProperty<T>, property: TemplateProperty)
}
