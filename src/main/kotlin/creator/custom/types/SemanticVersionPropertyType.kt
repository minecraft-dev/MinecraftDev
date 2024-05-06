package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateProperty
import com.demonwav.mcdev.util.SemanticVersion
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

class SemanticVersionPropertyType : PropertyType<SemanticVersion> {

    override fun createDefaultValue(raw: Any?): SemanticVersion =
        SemanticVersion.tryParse(raw as? String ?: "") ?: SemanticVersion(emptyList())

    override fun serialize(value: SemanticVersion): String = value.toString()

    override fun deserialize(string: String): SemanticVersion =
        SemanticVersion.tryParse(string) ?: SemanticVersion(emptyList())

    override fun Panel.buildUi(
        context: WizardContext,
        graphProperty: GraphProperty<SemanticVersion>,
        property: TemplateProperty
    ) {
        row(property.label) {
            textField().bindText(toStringProperty(graphProperty))
                .columns(COLUMNS_SHORT)
                .enabled(property.editable != false)
        }.visible(property.hidden != false)
    }

    override fun derive(
        property: GraphProperty<SemanticVersion>,
        parentValues: List<Any?>,
        properties: Map<String, Any?>,
        derivation: PropertyDerivation
    ): SemanticVersion {
        return when (derivation.method) {
            "extractVersionMajorMinor" -> extractVersionMajorMinor(parentValues[0])
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
}
