package com.demonwav.mcdev.platform.forge.sideonly;

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
}
