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

import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

class StringCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<String>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): String = raw as? String ?: ""

    override fun serialize(value: String): String = value

    override fun deserialize(string: String): String = string

    override fun toStringProperty(graphProperty: GraphProperty<String>) = graphProperty

    override fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): Any? {
        return when (derivation.method) {
            "suggestSpongePluginId" -> suggestSpongePluginId(parentValues.first())
            null -> deriveSelectFirst(parentValues, derivation).toString()
            else -> throw IllegalArgumentException("Unknown method derivation $derivation")
        }
    }

    private fun suggestSpongePluginId(projectName: Any?): String? {
        if (projectName !is String) {
            return null
        }

        val invalidModIdRegex = "[^a-z0-9-_]+".toRegex()
        val sanitized = projectName.lowercase().replace(invalidModIdRegex, "_")
        if (sanitized.length > 64) {
            return sanitized.substring(0, 64)
        }

        return sanitized
    }

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            val textField = textField().bindText(this@StringCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
            try {
                val regexString = descriptor.validator as? String
                if (regexString != null) {
                    val regex = regexString.toRegex()
                    textField.textValidation(BuiltinValidations.byRegex(regex))
                }
            } catch (t: Throwable) {
                if (t is ControlFlowException) {
                    throw t
                }
                logger<StringCreatorProperty>()
                    .error("Failed to create validator for property ${descriptor.name}", t)
            }
        }.visible(descriptor.hidden != true)
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = StringCreatorProperty(graph, descriptor, properties)
    }
}
