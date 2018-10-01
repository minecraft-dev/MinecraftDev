/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color

sealed class McpColorWarning {
    object NoHex : McpColorWarning()

    data class MissingComponents(val components: List<String>) : McpColorWarning()

    object MissingAlpha : McpColorWarning()

    object SuperfluousAlpha : McpColorWarning()

    data class ComponentOutOfRange(val min: String, val max: String, val clamp: (McpColorResult<Any>) -> Unit) : McpColorWarning()
}

