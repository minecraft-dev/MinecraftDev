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

package com.demonwav.mcdev.platform.mcp.gradle.tooling

import groovy.transform.CompileStatic

@CompileStatic
final class McpModelFG2Impl implements McpModelFG2, Serializable {

    final String minecraftVersion
    final String mcpVersion
    final Set<String> mappingFiles

    McpModelFG2Impl(String minecraftVersion, String mcpVersion, Set<String> mappingFiles) {
        this.minecraftVersion = minecraftVersion
        this.mcpVersion = mcpVersion
        this.mappingFiles = mappingFiles
    }
}
