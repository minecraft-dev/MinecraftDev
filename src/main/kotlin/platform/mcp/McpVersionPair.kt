/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
