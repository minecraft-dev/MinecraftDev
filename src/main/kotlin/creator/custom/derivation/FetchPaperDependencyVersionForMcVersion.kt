/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion

class FetchPaperDependencyVersionForMcVersion : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any {
        val version = parentValues[0] as SemanticVersion
        if (version < MinecraftVersions.MC26_1) {
            return "${version}-R0.1-SNAPSHOT";
        }

        val isMaven = (parentValues[1] as String) == "Maven"
        return if (isMaven) {
            "[${version}.build,)"
        } else {
            "${version}.build.+"
        }
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

            if (!parents[1]!!.acceptsType(String::class.java)) {
                reporter.error("Second parent must produce a string")
                return null
            }

            return FetchPaperDependencyVersionForMcVersion()
        }
    }
}
