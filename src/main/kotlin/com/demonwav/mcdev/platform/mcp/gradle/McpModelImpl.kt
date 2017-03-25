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

import java.io.Serializable

class McpModelImpl(override val minecraftVersion: String,
                   override val mcpVersion: String,
                   override val mappingFiles: Set<String>) : McpModel, Serializable
