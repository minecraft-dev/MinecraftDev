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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.PropertyDerivation
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter
import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates
import com.demonwav.mcdev.creator.custom.model.ClassFqn
import com.demonwav.mcdev.creator.custom.types.CreatorProperty
import com.demonwav.mcdev.util.capitalize
import com.demonwav.mcdev.util.decapitalize

class SuggestClassNamePropertyDerivation : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any {
        val coords = parentValues[0] as BuildSystemCoordinates
        val name = parentValues[1] as String
        val sanitizedName = name.split(NOT_JAVA_IDENTIFIER).joinToString("", transform = String::capitalize)
        return ClassFqn("${coords.groupId}.${sanitizedName.decapitalize()}.$sanitizedName")
    }

    companion object : PropertyDerivationFactory {

        private val NOT_JAVA_IDENTIFIER = Regex("\\P{javaJavaIdentifierPart}+")

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (parents == null || parents.size < 2) {
                reporter.error(MCDevBundle("creator.template.validation.expected_two_parents"))
                return null
            }

            if (parents.size > 2) {
                reporter.warn(MCDevBundle("creator.template.validation.more_than_two_parents"))
            }

            if (!parents[0]!!.acceptsType(BuildSystemCoordinates::class.java)) {
                reporter.error(MCDevBundle("creator.template.validation.first_parent_build_coordinates"))
                return null
            }

            if (!parents[1]!!.acceptsType(String::class.java)) {
                reporter.error(MCDevBundle("creator.template.validation.second_parent_string_value"))
                return null
            }

            return SuggestClassNamePropertyDerivation()
        }
    }
}
