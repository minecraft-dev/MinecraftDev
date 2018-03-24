/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling;

import java.util.Set;

public interface McpModel {
    String getMinecraftVersion();
    String getMcpVersion();
    Set<String> getMappingFiles();
}
