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

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.model.StringList
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class InlineStringListCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<StringList>(descriptor, graph, properties, StringList::class.java) {

    override fun createDefaultValue(raw: Any?): StringList = deserialize(raw as? String ?: "")

    override fun serialize(value: StringList): String = value.values.joinToString(transform = String::trim)

    override fun deserialize(string: String): StringList = string.split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .run(::StringList)

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            this.textField().bindText(this@InlineStringListCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
        }.propertyVisibility()
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = InlineStringListCreatorProperty(descriptor, graph, properties)
    }
}
