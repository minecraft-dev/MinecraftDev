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
import com.demonwav.mcdev.creator.custom.model.LicenseData
import com.demonwav.mcdev.util.License
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.transform
import com.intellij.ui.ComboboxSpeedSearch
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import java.time.ZonedDateTime

class LicenseCreatorProperty(
    descriptor: TemplatePropertyDescriptor,
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<LicenseData>(descriptor, graph, properties, LicenseData::class.java) {

    override val graphProperty: GraphProperty<LicenseData> =
        graph.property(createDefaultValue(descriptor.default))

    override fun createDefaultValue(raw: Any?): LicenseData =
        deserialize(raw as? String ?: License.ALL_RIGHTS_RESERVED.id)

    override fun serialize(value: LicenseData): String = value.id

    override fun deserialize(string: String): LicenseData =
        LicenseData(string, License.byId(string)?.toString() ?: string, ZonedDateTime.now().year.toString())

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.translatedLabel) {
            val model = EnumComboBoxModel(License::class.java)
            val licenseEnumProperty = graphProperty.transform(
                { License.byId(it.id) ?: License.entries.first() },
                { deserialize(it.id) }
            )
            comboBox(model)
                .bindItem(licenseEnumProperty)
                .also { ComboboxSpeedSearch.installOn(it.component) }
        }.enabled(descriptor.editable != false)
    }

    class Factory : CreatorPropertyFactory {

        override fun create(
            descriptor: TemplatePropertyDescriptor,
            graph: PropertyGraph,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = LicenseCreatorProperty(descriptor, graph, properties)
    }
}
