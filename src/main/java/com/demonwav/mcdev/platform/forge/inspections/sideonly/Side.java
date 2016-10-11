/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public enum Side {

    CLIENT("SideOnly.CLIENT"),
    SERVER("SideOnly.SERVER"),
    NONE("NONE"),
    INVALID("INVALID");

    @NotNull
    private final String name;

    Side(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public static final Key<Side> KEY = new Key<>("MC_DEV_SIDE");
}
