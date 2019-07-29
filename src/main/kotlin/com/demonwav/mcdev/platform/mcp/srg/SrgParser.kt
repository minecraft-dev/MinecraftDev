/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg

import java.nio.file.Path

interface SrgParser {
    fun parseSrg(path: Path): McpSrgMap
}
