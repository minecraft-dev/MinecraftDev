/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle.tooling;

import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModel;

import java.util.Set;

public interface ForgePatcherModel {
    McpModel getMcpModel();
    Set<String> getProjects();
}
