package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class StringPropertyType : PropertyType<String> {

    override fun createDefaultValue(raw: Any?): String = raw as? String ?: ""

    override fun serialize(value: String): String = value

    override fun deserialize(string: String): String = string

    override fun toStringProperty(graphProperty: GraphProperty<String>): ObservableMutableProperty<String> =
        graphProperty

    override fun Panel.buildUi(context: WizardContext, graphProperty: GraphProperty<String>, property: TemplateProperty) {
        row(property.label) {
            textField().bindText(toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(property.editable != false)
        }.visible(property.hidden != false)
    }
}
