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

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.derivation.PreparedDerivation
import com.demonwav.mcdev.creator.custom.derivation.RecommendJavaVersionForMcVersionPropertyDerivation
import com.demonwav.mcdev.creator.custom.derivation.SelectPropertyDerivation
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.columns

class IntegerCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<Int>(descriptor, graph, properties, Int::class.java) {

    override fun createDefaultValue(raw: Any?): Int = (raw as? Number)?.toInt() ?: 0

    override fun serialize(value: Int): String = value.toString()

    override fun deserialize(string: String): Int = string.toIntOrNull() ?: 0

    override fun convertSelectDerivationResult(original: Any?): Any? = (original as? Number)?.toInt()

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            this.intTextField().bindIntText(graphProperty)
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
        }.propertyVisibility()
    }

    override fun setupDerivation(
        reporter: TemplateValidationReporter,
        derives: PropertyDerivation
    ): PreparedDerivation? = when (derives.method) {
        "recommendJavaVersionForMcVersion" -> {
            val parents = collectDerivationParents(reporter)
            RecommendJavaVersionForMcVersionPropertyDerivation.create(reporter, parents, derives)
        }

        null -> {
            // No need to collect parent values for this one because it is not used
            SelectPropertyDerivation.create(reporter, emptyList(), derives)
        }

        else -> null
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = IntegerCreatorProperty(descriptor, graph, properties)
    }
}
