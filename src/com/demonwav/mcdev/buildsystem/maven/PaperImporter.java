package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.platform.bukkit.PaperModuleType;

import org.jetbrains.annotations.NotNull;

public class PaperImporter extends SpigotImporter {
    public PaperImporter() {
        super("com.destroystokyo.paper", "paper-api");
    }

    @NotNull
    @Override
    public PaperModuleType getModuleType() {
        return PaperModuleType.getInstance();
    }
}
