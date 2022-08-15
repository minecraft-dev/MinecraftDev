/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom

import groovy.transform.Immutable

@Immutable(knownImmutableClasses = [File])
class FabricLoomModelImpl implements FabricLoomModel, Serializable {
    File tinyMappings
    Map<String, List<DecompilerModel>> decompilers
    boolean splitMinecraftJar

    @Immutable
    static class DecompilerModelImpl implements DecompilerModel, Serializable {
        String name
        String taskName
        String sourcesPath
    }
}
