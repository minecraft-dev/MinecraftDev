package com.demonwav.mcdev.creator.custom.types.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.demonwav.mcdev.creator.custom.types.PropertyType
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.columns

class IntegerPropertyType : PropertyType<Int> {

    override fun createDefaultValue(raw: Any?): Int = raw as? Int ?: 0

    override fun serialize(value: Int): String = value.toString()

    override fun deserialize(string: String): Int = string.toIntOrNull() ?: 0

    override fun Panel.buildUi(context: WizardContext, graphProperty: GraphProperty<Int>, property: TemplateProperty) {
        row(property.label) {
            intTextField().bindIntText(graphProperty)
                .columns(COLUMNS_LARGE)
                .enabled(property.editable != false)
        }.visible(property.hidden != false)
    }
}
