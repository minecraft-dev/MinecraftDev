package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import org.jetbrains.annotations.NotNull;

public class SpongeMavenImporter extends MinecraftMavenImporter {
    public SpongeMavenImporter() {
        super("org.spongepowered", "spongeapi");
    }

    @NotNull
    @Override
    public SpongeModuleType getModuleType() {
        return SpongeModuleType.getInstance();
    }
}
