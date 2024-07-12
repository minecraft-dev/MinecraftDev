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
import com.demonwav.mcdev.creator.custom.derivation.ExtractVersionMajorMinorPropertyDerivation
import com.demonwav.mcdev.creator.custom.derivation.PreparedDerivation
import com.demonwav.mcdev.creator.custom.derivation.SelectPropertyDerivation
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

open class SemanticVersionCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<SemanticVersion>(descriptor, graph, properties, SemanticVersion::class.java) {

    override fun createDefaultValue(raw: Any?): SemanticVersion =
        SemanticVersion.tryParse(raw as? String ?: "") ?: SemanticVersion(emptyList())

    override fun serialize(value: SemanticVersion): String = value.toString()

    override fun deserialize(string: String): SemanticVersion =
        SemanticVersion.tryParse(string) ?: SemanticVersion(emptyList())

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            this.textField().bindText(this@SemanticVersionCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_SHORT)
                .enabled(descriptor.editable != false)
        }.propertyVisibility()
    }

    override fun setupDerivation(
        reporter: TemplateValidationReporter,
        derives: PropertyDerivation
    ): PreparedDerivation? = when (derives.method) {
        "extractVersionMajorMinor" -> {
            val parents = collectDerivationParents(reporter)
            ExtractVersionMajorMinorPropertyDerivation.create(reporter, parents, derives)
        }

        null -> {
            SelectPropertyDerivation.create(reporter, emptyList(), derives)
        }

        else -> null
    }

    override fun convertSelectDerivationResult(original: Any?): Any? {
        return (original as? String)?.let(SemanticVersion::tryParse)
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = SemanticVersionCreatorProperty(descriptor, graph, properties)
    }
}
