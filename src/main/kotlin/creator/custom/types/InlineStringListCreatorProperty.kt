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
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<StringList>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): StringList = deserialize(raw as? String ?: "")

    override fun serialize(value: StringList): String = value.values.joinToString(transform = String::trim)

    override fun deserialize(string: String): StringList = string.split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .run(::StringList)

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            this.textField().bindText(this@InlineStringListCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
        }.visible(descriptor.hidden != true)
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = InlineStringListCreatorProperty(graph, descriptor, properties)
    }
}
