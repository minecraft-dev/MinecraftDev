package com.demonwav.mcdev.platform.sponge.maven;

import com.demonwav.mcdev.buildsystem.maven.AbstractMavenImporter;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import org.jetbrains.annotations.NotNull;

public class SpongeMavenImporter extends AbstractMavenImporter {

    public SpongeMavenImporter() {
        super(SpongeModuleType.getInstance());
    }

    @NotNull
    @Override
    public SpongeModuleType getModuleType() {
        return SpongeModuleType.getInstance();
    }
}
