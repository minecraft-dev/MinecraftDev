/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version

import com.demonwav.mcdev.platform.mcp.McpVersionPair

class McpVersionEntry(val versionPair: McpVersionPair, val isRed: Boolean = false) {

    override fun toString(): String {
        return if (isRed) {
            RED_START + versionPair.mcpVersion + RED_END
        } else {
            versionPair.mcpVersion
        }
    }

    companion object {
        private const val RED_START = "<html><font color='red'>"
        private const val RED_END = "</font></html>"
    }
}
