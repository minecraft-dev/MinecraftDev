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
import com.intellij.icons.AllIcons
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.content.AlertIcon
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected

class BooleanCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<Boolean>(descriptor, graph, properties, Boolean::class.java) {

    override fun createDefaultValue(raw: Any?): Boolean = raw as? Boolean ?: false

    override fun serialize(value: Boolean): String = value.toString()

    override fun deserialize(string: String): Boolean = string.toBoolean()

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        val label = descriptor.translatedLabel
        panel.row(label) {
            val warning = descriptor.translatedWarning
            if (warning != null) {
                icon(AlertIcon(AllIcons.General.Warning))
                    .gap(RightGap.SMALL)
                    .comment(descriptor.translate(warning))
            }

            this.checkBox(label.removeSuffix(":").trim())
                .bindSelected(graphProperty)
                .enabled(descriptor.editable != false)
        }.propertyVisibility()
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = BooleanCreatorProperty(descriptor, graph, properties)
    }
}
