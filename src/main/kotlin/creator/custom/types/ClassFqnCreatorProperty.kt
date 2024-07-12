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
import com.demonwav.mcdev.creator.custom.derivation.SuggestClassNamePropertyDerivation
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

class ClassFqnCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<ClassFqn>(descriptor, graph, properties, ClassFqn::class.java) {

    override fun createDefaultValue(raw: Any?): ClassFqn = ClassFqn(raw as? String ?: "")

    override fun serialize(value: ClassFqn): String = value.toString()

    override fun deserialize(string: String): ClassFqn = ClassFqn(string)

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            this.textField().bindText(this@ClassFqnCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .textValidation(BuiltinValidations.validClassFqn)
                .enabled(descriptor.editable != false)
        }.propertyVisibility()
    }

    override fun setupDerivation(
        reporter: TemplateValidationReporter,
        derives: PropertyDerivation
    ): PreparedDerivation? = when (derives.method) {
        "suggestClassName" -> {
            val parents = collectDerivationParents(reporter)
            SuggestClassNamePropertyDerivation.create(reporter, parents, derives)
        }

        else -> null
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = ClassFqnCreatorProperty(descriptor, graph, properties)
    }
}
