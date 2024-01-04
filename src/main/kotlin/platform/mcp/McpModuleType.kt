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

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.mappings.getMappedClass
import com.demonwav.mcdev.platform.mcp.mappings.getMappedField
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.util.CommonColors
import com.intellij.openapi.module.Module
import java.awt.Color
import javax.swing.Icon

object McpModuleType : AbstractModuleType<McpModule>("", "") {

    private const val ID = "MCP_MODULE_TYPE"

    init {
        CommonColors.applyStandardColors(colorMap, McpConstants.CHAT_FORMATTING)
    }

    override fun classToColorMappings(module: Module): Map<String, Color> {
        return colorMap.mapKeys { key ->
            val parts = key.key.split('.')
            val className = parts.dropLast(1).joinToString(".")
            val fieldName = parts.last()
            "${module.getMappedClass(className)}.${module.getMappedField(className, fieldName)}"
        }
    }

    override val platformType = PlatformType.MCP
    override val icon: Icon? = null
    override val id = ID
    override val ignoredAnnotations = emptyList<String>()
    override val listenerAnnotations = emptyList<String>()
    override val hasIcon = false

    override fun generateModule(facet: MinecraftFacet) = McpModule(facet)
}
