/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.version

class McpVersionEntry(val text: String, val isRed: Boolean = false) {

    override fun toString(): String {
        return if (isRed) {
            RED_START + text + RED_END
        } else {
            text
        }
    }

    companion object {
        private const val RED_START = "<html><font color='red'>"
        private const val RED_END = "</font></html>"
    }
}
