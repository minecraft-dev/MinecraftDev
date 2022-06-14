/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom

class FabricLoomModelImpl implements FabricLoomModel, Serializable {

    private final File tinyMappings
    private final Map<String, String> decompilers

    FabricLoomModelImpl(File tinyMappings, Map<String, String> decompilers) {
        this.tinyMappings = tinyMappings
        this.decompilers = decompilers
    }

    @Override
    File getTinyMappings() {
        return tinyMappings
    }

    @Override
    Map<String, String> getDecompilers() {
        return decompilers
    }
}
