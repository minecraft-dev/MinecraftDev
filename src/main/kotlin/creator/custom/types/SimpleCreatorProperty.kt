package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem

abstract class SimpleCreatorProperty<T>(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<T>(descriptor, graph, properties) {

    private val isDropdown = !descriptor.options.isNullOrEmpty()
    private val options = descriptor.options?.filterIsInstance<String>()?.map(::deserialize) ?: emptyList()
    private val defaultOptionIndex = if (isDropdown) descriptor.default as? Int ?: 0 else null
    private val defaultValue by lazy { createDefaultValue(if (isDropdown) descriptor.options!![defaultOptionIndex!!] else descriptor.default) }

    override val graphProperty: GraphProperty<T> by lazy { graph.property(defaultValue) }

    override fun buildUi(panel: Panel, context: WizardContext) {
        if (isDropdown) {
            panel.row(descriptor.label) {
                comboBox(options)
                    .bindItem(graphProperty)
                    .enabled(descriptor.editable != false)
                    .also { ComboboxSpeedSearch.installOn(it.component) }
            }.visible(descriptor.hidden != true)
        } else {
            buildSimpleUi(panel, context)
        }
    }

    abstract fun buildSimpleUi(panel: Panel, context: WizardContext)
}
