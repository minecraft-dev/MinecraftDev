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

class LicenseProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : CreatorProperty<LicenseData>(descriptor, graph, properties) {

    override val graphProperty: GraphProperty<LicenseData> =
        graph.property(createDefaultValue(descriptor.default))

    override fun createDefaultValue(raw: Any?): LicenseData =
        deserialize(raw as? String ?: License.ALL_RIGHTS_RESERVED.id)

    override fun serialize(value: LicenseData): String = value.id

    override fun deserialize(string: String): LicenseData =
        LicenseData(string, ZonedDateTime.now().year.toString())

    override fun buildUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
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
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = LicenseProperty(graph, descriptor, properties)
    }
}
