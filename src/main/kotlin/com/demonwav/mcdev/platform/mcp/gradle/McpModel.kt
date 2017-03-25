/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle

interface McpModel {

    val minecraftVersion: String
    val mcpVersion: String
    val mappingFiles: Set<String>
}
