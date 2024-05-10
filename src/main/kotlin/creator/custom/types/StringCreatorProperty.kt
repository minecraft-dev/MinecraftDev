package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class StringCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<String>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): String = raw as? String ?: ""

    override fun serialize(value: String): String = value

    override fun deserialize(string: String): String = string

    override fun toStringProperty(graphProperty: GraphProperty<String>) = graphProperty

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            this.textField().bindText(this@StringCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
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
