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

import com.demonwav.mcdev.util.SemanticVersion

data class McpVersionPair(val mcpVersion: String, val mcVersion: SemanticVersion) : Comparable<McpVersionPair> {

    override fun compareTo(other: McpVersionPair): Int {
        val mcRes = mcVersion.compareTo(other.mcVersion)
        if (mcRes != 0) {
            return mcRes
        }
        val thisStable = mcpVersion.startsWith("stable")
        val thatStable = other.mcpVersion.startsWith("stable")
        return if (thisStable && !thatStable) {
            -1
        } else if (!thisStable && thatStable) {
            1
        } else {
            val thisNum = mcpVersion.substringAfter('_')
            val thatNum = other.mcpVersion.substringAfter('_')
            thisNum.toInt().compareTo(thatNum.toInt())
        }
    }
}
