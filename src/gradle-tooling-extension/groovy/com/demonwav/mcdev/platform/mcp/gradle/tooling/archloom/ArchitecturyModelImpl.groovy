/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom

class ArchitecturyModelImpl implements ArchitecturyModel, Serializable {
    private final ModuleType moduleType;

    ArchitecturyModelImpl(ModuleType moduleType) {
        this.moduleType = moduleType
    }

    ModuleType getModuleType() {
        return moduleType
    }
}
