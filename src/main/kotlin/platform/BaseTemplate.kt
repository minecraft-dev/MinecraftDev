/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project

abstract class BaseTemplate {

    protected fun Project.applyTemplate(
        templateName: String,
        properties: Map<String, *>? = null,
    ): String {
        val manager = FileTemplateManager.getInstance(this)
        val template = manager.getJ2eeTemplate(templateName)

        val allProperties = manager.defaultProperties.toMutableMap()
        properties?.let { prop -> allProperties.putAll(prop) }

        return template.getText(allProperties)
    }
}
