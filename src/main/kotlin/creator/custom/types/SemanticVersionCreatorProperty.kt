package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

open class SemanticVersionCreatorProperty(
    graph: PropertyGraph,
    descriptor: TemplatePropertyDescriptor,
    properties: Map<String, CreatorProperty<*>>
) : SimpleCreatorProperty<SemanticVersion>(graph, descriptor, properties) {

    override fun createDefaultValue(raw: Any?): SemanticVersion =
        SemanticVersion.tryParse(raw as? String ?: "") ?: SemanticVersion(emptyList())

    override fun serialize(value: SemanticVersion): String = value.toString()

    override fun deserialize(string: String): SemanticVersion =
        SemanticVersion.tryParse(string) ?: SemanticVersion(emptyList())

    override fun buildSimpleUi(panel: Panel, context: WizardContext) {
        panel.row(descriptor.label) {
            this.textField().bindText(this@SemanticVersionCreatorProperty.toStringProperty(graphProperty))
                .columns(COLUMNS_SHORT)
                .enabled(descriptor.editable != false)
        }.visible(descriptor.hidden != true)
    }

    override fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): Any {
        return when (derivation.method) {
            "extractVersionMajorMinor" -> extractVersionMajorMinor(parentValues[0])
            null -> SemanticVersion.parse(deriveSelectFirst(parentValues, derivation).toString())
            else -> throw IllegalArgumentException("Unknown method derivation $derivation")
        }
    }

    private fun extractVersionMajorMinor(from: Any?): SemanticVersion {
        if (from !is SemanticVersion) {
            return SemanticVersion(emptyList())
        }

        if (from.parts.size < 2) {
            return SemanticVersion(emptyList())
        }

        val (part1, part2) = from.parts
        if (part1 is SemanticVersion.Companion.VersionPart.ReleasePart &&
            part2 is SemanticVersion.Companion.VersionPart.ReleasePart
        ) {
            return SemanticVersion(listOf(part1, part2))
        }

        return SemanticVersion(emptyList())
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = SemanticVersionCreatorProperty(graph, descriptor, properties)
    }
}
