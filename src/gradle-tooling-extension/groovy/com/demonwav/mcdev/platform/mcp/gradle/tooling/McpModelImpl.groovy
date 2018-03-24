/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling

import groovy.transform.CompileStatic

@CompileStatic
final class McpModelImpl implements McpModel, Serializable {

    final String minecraftVersion
    final String mcpVersion
    final Set<String> mappingFiles

    McpModelImpl(String minecraftVersion, String mcpVersion, Set<String> mappingFiles) {
        this.minecraftVersion = minecraftVersion
        this.mcpVersion = mcpVersion
        this.mappingFiles = mappingFiles
    }
}
