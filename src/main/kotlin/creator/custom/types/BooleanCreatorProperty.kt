package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected

class BooleanCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<Boolean>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): Boolean = raw as? Boolean ?: false

    override fun serialize(value: Boolean): String = value.toString()

    override fun deserialize(string: String): Boolean = string.toBoolean()

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            this.checkBox(descriptor.label)
                .bindSelected(graphProperty)
                .enabled(descriptor.editable != false)
        }.visible(descriptor.hidden != true)
    }
}
