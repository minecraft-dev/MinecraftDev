/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling

import groovy.transform.CompileStatic

@CompileStatic
final class McpModelFG3Impl implements McpModelFG3, Serializable {

    final List<String> minecraftDepVersions
    final String mcpVersion
    final File taskOutputLocation
    final String taskName

    McpModelFG3Impl(List<String> minecraftDepVersions, String mcpVersion, File taskOutputLocation, String taskName) {
        this.minecraftDepVersions = minecraftDepVersions
        this.mcpVersion = mcpVersion
        this.taskOutputLocation = taskOutputLocation
        this.taskName = taskName
    }
}
