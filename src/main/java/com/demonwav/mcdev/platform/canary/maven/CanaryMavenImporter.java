package com.demonwav.mcdev.platform.canary.maven;

import com.demonwav.mcdev.buildsystem.maven.AbstractMavenImporter;
import com.demonwav.mcdev.platform.canary.CanaryModuleType;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;

public class CanaryMavenImporter extends AbstractMavenImporter {

    public CanaryMavenImporter() {
        this(CanaryModuleType.getInstance());
    }

    protected CanaryMavenImporter(@NotNull final CanaryModuleType type) {
        super(type);
    }

    @NotNull
    @Override
    public ModuleType getModuleType() {
        return JavaModuleType.getModuleType();
    }

}
