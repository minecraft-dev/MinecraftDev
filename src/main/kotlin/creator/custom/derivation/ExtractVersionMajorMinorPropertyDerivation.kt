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

package com.demonwav.mcdev.creator.custom.derivation

import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.util.SemanticVersion

class ExtractVersionMajorMinorPropertyDerivation : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any? {
        val from = parentValues[0] as SemanticVersion
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

    companion object : PropertyDerivationFactory {

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (parents.isNullOrEmpty()) {
                reporter.error("Expected a parent")
                return null
            }

            if (!parents[0]!!.acceptsType(SemanticVersion::class.java)) {
                reporter.error("First parent must produce a semantic version")
                return null
            }

            return ExtractVersionMajorMinorPropertyDerivation()
        }
    }
}
