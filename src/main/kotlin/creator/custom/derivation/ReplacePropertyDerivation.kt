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
import com.demonwav.mcdev.creator.custom.types.CreatorProperty

class ReplacePropertyDerivation(
    val regex: Regex,
    val replacement: String,
    val maxLength: Int?,
    val lowercase: Boolean,
) : PreparedDerivation {

    override fun derive(parentValues: List<Any?>): Any? {
        val projectName = parentValues.first() as? String
            ?: return null

        var sanitized = projectName
        if (lowercase) {
            sanitized = sanitized.lowercase()
        }

        sanitized = sanitized.replace(regex, replacement)
        if (maxLength != null && sanitized.length > maxLength) {
            return sanitized.substring(0, maxLength)
        }

        return sanitized
    }

    companion object : PropertyDerivationFactory {

        override fun create(
            reporter: TemplateValidationReporter,
            parents: List<CreatorProperty<*>?>?,
            derivation: PropertyDerivation
        ): PreparedDerivation? {
            if (derivation.parameters == null) {
                reporter.error(MCDevBundle("creator.template.validation.missing_parameters"))
                return null
            }

            if (parents.isNullOrEmpty()) {
                reporter.error(MCDevBundle("creator.template.validation.missing_parent_value"))
                return null
            }

            if (parents.size > 1) {
                reporter.warn(MCDevBundle("creator.template.validation.more_than_one_parent"))
            }

            if (!parents[0]!!.acceptsType(String::class.java)) {
                reporter.error(MCDevBundle("creator.template.validation.parent_string"))
                return null
            }

            val regexString = derivation.parameters["regex"] as? String
            if (regexString == null) {
                reporter.error(MCDevBundle("creator.template.validation.missing_string_parameter", "regex"))
                return null
            }

            val regex = try {
                Regex(regexString)
            } catch (t: Throwable) {
                reporter.error(MCDevBundle("creator.template.validation.invalid_regex", regexString, t.message))
                return null
            }

            val replacement = derivation.parameters["replacement"] as? String
            if (replacement == null) {
                reporter.error(MCDevBundle("creator.template.validation.missing_string_parameter", "replacement"))
                return null
            }

            val maxLength = (derivation.parameters["maxLength"] as? Number)?.toInt()
            val lowercase = derivation.parameters["lowercase"] as? Boolean == true
            return ReplacePropertyDerivation(regex, replacement, maxLength, lowercase)
        }
    }
}
