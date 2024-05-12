package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.Panel

class ExternalCreatorProperty<T>(
    graph: PropertyGraph,
    properties: Map<String, CreatorProperty<*>>,
    override val graphProperty: GraphProperty<T>,
    descriptor: TemplatePropertyDescriptor = TemplatePropertyDescriptor("", "", "", null, null, null, null, null, "", null, null),
) : CreatorProperty<T>(descriptor, graph, properties) {

    override fun setupProperty() = Unit

    override fun createDefaultValue(raw: Any?): T = throw UnsupportedOperationException("Unsupported for external properties")

    override fun serialize(value: T): String = throw UnsupportedOperationException("Unsupported for external properties")

    override fun deserialize(string: String): T = throw UnsupportedOperationException("Unsupported for external properties")

    override fun buildUi(panel: Panel, context: WizardContext) = Unit
}
