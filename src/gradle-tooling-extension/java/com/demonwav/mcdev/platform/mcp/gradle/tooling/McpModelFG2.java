/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling;

import java.util.Set;

public interface McpModelFG2 extends McpModel {
    String getMinecraftVersion();
    Set<String> getMappingFiles();
}
