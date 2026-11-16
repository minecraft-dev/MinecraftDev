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

package com.demonwav.mcdev.creator.custom.finalizers

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.TemplateValidationReporter

interface AddRunConfigFinalizer : CreatorFinalizer {

    val executablesName: String
    val Map<String, Any>.executables: List<String>
        @Suppress("UNCHECKED_CAST")
        get() = this[executablesName] as List<String>

    override fun validate(
        reporter: TemplateValidationReporter,
        properties: Map<String, Any>
    ) {
        @Suppress("UNCHECKED_CAST")
        val executables = properties[executablesName] as? List<String>
        if (executables == null) {
            reporter.warn(MCDevBundle("creator.template.validation.missing_executables", executables))
        }

        @Suppress("UNCHECKED_CAST")
        val name = properties["name"] as? String
        if (name == null) {
            reporter.warn(MCDevBundle("creator.template.validation.missing_task_name"))
        }
    }
}
