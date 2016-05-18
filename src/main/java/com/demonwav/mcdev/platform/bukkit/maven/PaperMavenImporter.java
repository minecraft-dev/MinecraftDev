package com.demonwav.mcdev.platform.bukkit.maven;

import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import org.jetbrains.annotations.NotNull;

public class PaperMavenImporter extends SpigotMavenImporter {
    public PaperMavenImporter() {
        super(PaperModuleType.getInstance());
    }

    @NotNull
    @Override
    public PaperModuleType getModuleType() {
        return PaperModuleType.getInstance();
    }
}
