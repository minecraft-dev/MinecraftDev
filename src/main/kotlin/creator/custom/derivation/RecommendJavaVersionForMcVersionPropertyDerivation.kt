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
import com.demonwav.mcdev.creator.custom.model.HasMinecraftVersion
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion

class RecommendJavaVersionForMcVersionPropertyDerivation(val default: Int) : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any? {
        val mcVersion: SemanticVersion = when (val version = parentValues[0]) {
            is SemanticVersion -> version
            is HasMinecraftVersion -> version.minecraftVersion
            else -> return default
        }
        return MinecraftVersions.requiredJavaVersion(mcVersion).ordinal
    }

    companion object : PropertyDerivationFactory {

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (parents.isNullOrEmpty()) {
                reporter.error("Expected one parent")
                return null
            }

            if (parents.size > 1) {
                reporter.warn("More than one parent defined")
            }

            val parentValue = parents[0]!!
            if (!parentValue.acceptsType(SemanticVersion::class.java) &&
                !parentValue.acceptsType(HasMinecraftVersion::class.java)
            ) {
                reporter.error("Parent must produce a semantic version or a value that has a Minecraft version")
                return null
            }

            val default = (derivation.default as? Number)?.toInt()
            if (default == null) {
                reporter.error("Default value is required and must be an integer")
                return null
            }

            return RecommendJavaVersionForMcVersionPropertyDerivation(default)
        }
    }
}
