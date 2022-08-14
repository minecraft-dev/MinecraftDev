/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom;

public interface ArchitecturyModel {
    ModuleType getModuleType();

    enum ModuleType {
        NONE, COMMON, PLATFORM_SPECIFIC
    }
}
