package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class ClassFqnPropertyType : PropertyType<ClassFqn> {

    override fun createDefaultValue(raw: Any?): ClassFqn = ClassFqn(raw as? String ?: "")

    override fun serialize(value: ClassFqn): String = value.toString()

    override fun deserialize(string: String): ClassFqn = ClassFqn(string)

    override fun Panel.buildUi(graphProperty: GraphProperty<ClassFqn>, property: TemplateProperty) {
        row(property.label) {
            textField().bindText(toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(property.editable != false)
        }.visible(property.hidden != false)
    }
}
