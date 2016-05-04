package com.demonwav.mcdev.buildsystem.maven;

import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import org.jetbrains.annotations.NotNull;

public class SpongeImporter extends MinecraftImporter {
    public SpongeImporter() {
        super("org.spongepowered", "spongeapi");
    }

    @NotNull
    @Override
    public SpongeModuleType getModuleType() {
        return SpongeModuleType.getInstance();
    }
}
