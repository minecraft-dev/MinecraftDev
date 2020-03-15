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

import java.io.File;
import java.util.List;

public interface McpModelFG3 extends McpModel {
    List<String> getMinecraftDepVersions();
    File getTaskOutputLocation();
    String getTaskName();
}
