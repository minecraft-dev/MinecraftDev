package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

abstract class SimpleCreatorProperty<T>(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<T>(descriptor, graph, properties) {

    private val options: Map<T, String>? = makeOptionsList()

    private fun makeOptionsList(): Map<T, String>? {
        val map = when (val options = descriptor.options) {
            is Map<*, *> -> options.mapValues { it.value.toString() }
            is Iterable<*> -> options.associateWithTo(linkedMapOf()) { it.toString() }
            else -> null
        }

        return map?.mapKeys {
            @Suppress("UNCHECKED_CAST")
            when (val key = it.key) {
                is String -> deserialize(key)
                else -> key
            } as T
        }
    }

    private val isDropdown = !options.isNullOrEmpty()
    private val defaultValue by lazy {
        val raw = if (isDropdown) {
            if (descriptor.default is Number && descriptor.options is List<*>) {
                descriptor.options[descriptor.default.toInt()]
            } else {
                options!![createDefaultValue(descriptor.default)]
            }
        } else {
            descriptor.default
        }

        createDefaultValue(raw)
    }

    override val graphProperty: GraphProperty<T> by lazy { graph.property(defaultValue) }

    override fun buildUi(panel: Panel, context: WizardContext) {
        if (isDropdown) {
            panel.row(descriptor.label) {
                comboBox(options!!.keys, DropdownAutoRenderer())
                    .bindItem(graphProperty)
                    .enabled(descriptor.editable != false)
                    .also { ComboboxSpeedSearch.installOn(it.component) }
            }.visible(descriptor.hidden != true)
        } else {
            buildSimpleUi(panel, context)
        }
    }

    abstract fun buildSimpleUi(panel: Panel, context: WizardContext)

    private inner class DropdownAutoRenderer : DefaultListCellRenderer() {

        override fun getListCellRendererComponent(
            list: JList<out Any?>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val label = options!![value] ?: value.toString()
            return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
        }
    }
}
