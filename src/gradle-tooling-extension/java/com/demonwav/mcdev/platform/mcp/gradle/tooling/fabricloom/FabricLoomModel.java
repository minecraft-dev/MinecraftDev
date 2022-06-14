/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom;

import java.io.File;
import java.util.Map;

public interface FabricLoomModel {

    File getTinyMappings();

    Map<String, String> getDecompilers();
}
