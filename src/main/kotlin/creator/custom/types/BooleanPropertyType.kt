package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected

class BooleanPropertyType : PropertyType<Boolean> {

    override fun createDefaultValue(raw: Any?): Boolean = raw as? Boolean ?: false

    override fun serialize(value: Boolean): String = value.toString()

    override fun deserialize(string: String): Boolean = string.toBoolean()

    override fun Panel.buildUi(graphProperty: GraphProperty<Boolean>, property: TemplateProperty) {
        row(property.label) {
            checkBox(property.label)
                .bindSelected(graphProperty)
                .enabled(property.editable != false)
        }.visible(property.hidden != false)
    }
}
