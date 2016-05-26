package com.demonwav.mcdev.platform.sponge.maven;

import com.demonwav.mcdev.buildsystem.maven.AbstractMavenImporter;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;

public class SpongeMavenImporter extends AbstractMavenImporter {

    public SpongeMavenImporter() {
        super(SpongeModuleType.getInstance());
    }

    @NotNull
    @Override
    public ModuleType getModuleType() {
        return JavaModuleType.getModuleType();
    }
}
