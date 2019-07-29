/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.util.SemanticVersion

data class McpVersionPair(val mcpVersion: String, val mcVersion: String) : Comparable<McpVersionPair> {
    override fun compareTo(other: McpVersionPair): Int {
        val mcRes = SemanticVersion.parse(mcVersion).compareTo(SemanticVersion.parse(other.mcVersion))
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
