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
import com.demonwav.mcdev.util.MinecraftVersions
import com.demonwav.mcdev.util.SemanticVersion

class ExtractPaperApiVersionPropertyDerivation : ExtractVersionMajorMinorPropertyDerivation() {

    override fun derive(parentValues: List<Any?>): Any? {
        val from = parentValues[0] as SemanticVersion
        if (from >= MinecraftVersions.MC1_20_5) {
            return from
        }

        return super.derive(parentValues)
    }

  companion object : PropertyDerivationFactory {

    override fun create(
      reporter: TemplateValidationReporter,
      parents: List<CreatorProperty<*>?>?,
      derivation: PropertyDerivation
    ): PreparedDerivation? {
      if (parents.isNullOrEmpty()) {
        reporter.error(MCDevBundle("creator.template.validation.expected_parent"))
        return null
      }

      if (!parents[0]!!.acceptsType(SemanticVersion::class.java)) {
        reporter.error(MCDevBundle("creator.template.validation.first_parent_semantic_version"))
        return null
      }

      return ExtractPaperApiVersionPropertyDerivation()
    }
  }
}
