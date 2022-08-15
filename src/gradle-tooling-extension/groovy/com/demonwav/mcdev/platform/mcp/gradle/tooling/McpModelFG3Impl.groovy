/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
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
    final List<File> accessTransformers

    McpModelFG3Impl(
            final List<String> minecraftDepVersions,
            final String mcpVersion,
            final File taskOutputLocation,
            final String taskName,
            final List<File> accessTransformers
    ) {
        this.minecraftDepVersions = minecraftDepVersions
        this.mcpVersion = mcpVersion
        this.taskOutputLocation = taskOutputLocation
        this.taskName = taskName
        this.accessTransformers = accessTransformers
    }
}
