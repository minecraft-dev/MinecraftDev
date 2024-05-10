package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.columns

class IntegerCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<Int>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): Int = raw as? Int ?: 0

    override fun serialize(value: Int): String = value.toString()

    override fun deserialize(string: String): Int = string.toIntOrNull() ?: 0

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            this.intTextField().bindIntText(graphProperty)
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
        }.visible(descriptor.hidden != true)
    }

    override fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): Any? {
        return when (derivation.method) {
            "recommendJavaVersionForMcVersion" -> recommendJavaVersionForMcVersion(parentValues[0])
            else -> throw IllegalArgumentException("Unknown method derivation $derivation")
        }
    }

    private fun recommendJavaVersionForMcVersion(from: Any?): Int {
        if (from !is SemanticVersion) {
            return 17
        }

        return MinecraftVersions.requiredJavaVersion(from).ordinal
    }
}
