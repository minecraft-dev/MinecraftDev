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
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.derivation.PreparedDerivation
import com.demonwav.mcdev.creator.custom.derivation.ReplacePropertyDerivation
import com.demonwav.mcdev.creator.custom.derivation.SelectPropertyDerivation
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

class StringCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<String>(descriptor, graph, properties, String::class.java) {

    private var validationRegex: Regex? = null

    override fun createDefaultValue(raw: Any?): String = raw as? String ?: ""

    override fun serialize(value: String): String = value

    override fun deserialize(string: String): String = string

    override fun toStringProperty(graphProperty: GraphProperty<String>) = graphProperty

    override fun setupProperty(reporter: TemplateValidationReporter) {
        super.setupProperty(reporter)

        val regexString = descriptor.validator as? String
        if (regexString != null) {
            try {
                validationRegex = regexString.toRegex()
            } catch (t: Throwable) {
                reporter.error("Invalid validator regex: '$regexString': ${t.message}")
            }
        }
    }

    override fun setupDerivation(
        reporter: TemplateValidationReporter,
        derives: PropertyDerivation
    ): PreparedDerivation? = when (derives.method) {
        "replace" -> {
            val parents = collectDerivationParents(reporter)
            ReplacePropertyDerivation.create(reporter, parents, derives)
        }

        null -> {
            // No need to collect parent values for this one because it is not used
            SelectPropertyDerivation.create(reporter, emptyList(), derives)
        }

        else -> null
    }

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            val textField = textField().bindText(this@StringCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
            if (validationRegex != null) {
                textField.textValidation(BuiltinValidations.byRegex(validationRegex!!))
            }
        }.propertyVisibility()
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = StringCreatorProperty(descriptor, graph, properties)
    }
}
