package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.demonwav.mcdev.util.capitalize
import com.demonwav.mcdev.util.decapitalize
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

class ClassFqnCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<ClassFqn>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): ClassFqn = ClassFqn(raw as? String ?: "")

    override fun serialize(value: ClassFqn): String = value.toString()

    override fun deserialize(string: String): ClassFqn = ClassFqn(string)

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            this.textField().bindText(this@ClassFqnCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .textValidation(BuiltinValidations.validClassFqn)
                .enabled(descriptor.editable != false)
        }.visible(descriptor.hidden != true)
    }

    override fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): ClassFqn {
        return when (derivation.method) {
            "suggestClassName" -> suggestClassName(parentValues)
            null -> ClassFqn(deriveSelectFirst(parentValues, derivation).toString())
            else -> throw IllegalArgumentException("Unknown method derivation $derivation")
        }
    }

    private fun suggestClassName(parentValues: List<Any?>): ClassFqn {
        val coords = parentValues.getOrNull(0) as? BuildSystemCoordinates
            ?: throw RuntimeException("Expected parent 0 to be a build system coordinates")
        val name = parentValues.getOrNull(1) as? String
            ?: throw RuntimeException("Expected parent 1 to be a string")
        return ClassFqn("${coords.groupId}.${name.decapitalize()}.${name.capitalize()}")
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = ClassFqnCreatorProperty(graph, descriptor, properties)
    }
}
