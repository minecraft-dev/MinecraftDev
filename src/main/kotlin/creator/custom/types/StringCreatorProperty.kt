package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.textValidation

class StringCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<String>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): String = raw as? String ?: ""

    override fun serialize(value: String): String = value

    override fun deserialize(string: String): String = string

    override fun toStringProperty(graphProperty: GraphProperty<String>) = graphProperty

    override fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): Any? {
        return when (derivation.method) {
            "suggestSpongePluginId" -> suggestSpongePluginId(parentValues.first())
            else -> throw IllegalArgumentException("Unknown method derivation $derivation")
        }
    }

    private fun suggestSpongePluginId(projectName: Any?): String? {
        if (projectName !is String) {
            return null
        }

        val invalidModIdRegex = "[^a-z0-9-_]+".toRegex()
        val sanitized = projectName.lowercase().replace(invalidModIdRegex, "_")
        if (sanitized.length > 64) {
            return sanitized.substring(0, 64)
        }

        return sanitized
    }

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            val textField = textField().bindText(this@StringCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
            try {
                val regexString = descriptor.validator as? String
                if (regexString != null) {
                    val regex = regexString.toRegex()
                    textField.textValidation(BuiltinValidations.byRegex(regex))
                }
            } catch (e: Exception) {
                logger<StringCreatorProperty>()
                    .error("Failed to create validator for property ${descriptor.name}", e)
            }
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
