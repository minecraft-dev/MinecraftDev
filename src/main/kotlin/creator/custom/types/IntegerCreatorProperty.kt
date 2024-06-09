/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.creator.custom.types

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor
import com.demonwav.mcdev.creator.custom.model.HasMinecraftVersion
import com.demonwav.mcdev.platform.sponge.util.SpongeVersions
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
        panel.row(descriptor.translatedLabel) {
            this.intTextField().bindIntText(graphProperty)
                .columns(COLUMNS_LARGE)
                .enabled(descriptor.editable != false)
        }.visible(descriptor.hidden != true)
    }

    override fun derive(parentValues: List<Any?>, derivation: PropertyDerivation): Any? {
        return when (derivation.method) {
            "recommendJavaVersionForMcVersion" -> recommendJavaVersionForMcVersion(parentValues[0])
            "recommendJavaVersionForSpongeApiVersion" -> recommendJavaVersionForSpongeApiVersion(parentValues[0])
            null -> (deriveSelectFirst(parentValues, derivation) as Number).toInt()
            else -> throw IllegalArgumentException("Unknown method derivation $derivation")
        }
    }

    private fun recommendJavaVersionForMcVersion(from: Any?): Int {
        if (from is SemanticVersion) {
            return MinecraftVersions.requiredJavaVersion(from).ordinal
        }

        if (from is HasMinecraftVersion) {
            return recommendJavaVersionForMcVersion(from.minecraftVersion)
        }

        return 17
    }

    private fun recommendJavaVersionForSpongeApiVersion(from: Any?): Int {
        if (from !is SemanticVersion) {
            return 17
        }

        return SpongeVersions.requiredJavaVersion(from).ordinal
    }

    class Factory : CreatorPropertyFactory {
        override fun create(
            graph: PropertyGraph,
            descriptor: TemplatePropertyDescriptor,
            properties: Map<String, CreatorProperty<*>>
        ): CreatorProperty<*> = IntegerCreatorProperty(graph, descriptor, properties)
    }
}
