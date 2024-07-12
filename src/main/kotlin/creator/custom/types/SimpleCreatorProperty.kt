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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.BuiltinValidations
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
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>,
    valueType: Class<T>
) : CreatorProperty<T>(descriptor, graph, properties, valueType) {

    private val options: Map<T, String>? = makeOptionsList()

    private fun makeOptionsList(): Map<T, String>? {
        val map = when (val options = descriptor.options) {
            is Map<*, *> -> options.mapValues { descriptor.translate(it.value.toString()) }
            is Iterable<*> -> options.associateWithTo(linkedMapOf()) {
                val optionKey = it.toString()
                descriptor.translateOrNull("creator.ui.${descriptor.name.lowercase()}.option.${optionKey.lowercase()}")
                    ?: optionKey
            }

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
                descriptor.default ?: options?.keys?.firstOrNull()
            }
        } else {
            descriptor.default
        }

        createDefaultValue(raw)
    }

    override val graphProperty: GraphProperty<T> by lazy { graph.property(defaultValue) }

    override fun buildUi(panel: Panel, context: WizardContext) {
        if (isDropdown) {
            if (graphProperty.get() !in options!!.keys) {
                graphProperty.set(defaultValue)
            }

            panel.row(descriptor.translatedLabel) {
                if (descriptor.forceDropdown == true) {
                    comboBox(options.keys, DropdownAutoRenderer())
                        .bindItem(graphProperty)
                        .enabled(descriptor.editable != false)
                        .also {
                            val component = it.component
                            ComboboxSpeedSearch.installOn(component)
                            val validation =
                                BuiltinValidations.isAnyOf(component::getSelectedItem, options.keys, component)
                            it.validationOnInput(validation)
                            it.validationOnApply(validation)
                        }
                } else {
                    segmentedButton(options.keys) { options[it] ?: it.toString() }
                        .bind(graphProperty)
                        .enabled(descriptor.editable != false)
                        .maxButtonsCount(4)
                        .validation {
                            val message = MCDevBundle("creator.validation.invalid_option")
                            addInputRule(message) { it.selectedItem !in options.keys }
                            addApplyRule(message) { it.selectedItem !in options.keys }
                        }
                }
            }.propertyVisibility()
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
