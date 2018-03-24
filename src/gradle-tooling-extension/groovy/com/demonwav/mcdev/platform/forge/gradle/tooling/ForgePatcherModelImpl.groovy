/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle.tooling

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModel
import groovy.transform.CompileStatic

@CompileStatic
final class ForgePatcherModelImpl implements ForgePatcherModel, Serializable {

    final McpModel mcpModel
    final Set<String> projects

    ForgePatcherModelImpl(McpModel mcpModel, Set<String> projects) {
        this.mcpModel = mcpModel
        this.projects = projects
    }
}
