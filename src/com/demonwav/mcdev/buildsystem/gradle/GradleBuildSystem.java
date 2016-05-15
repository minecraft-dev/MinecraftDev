package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

public class GradleBuildSystem extends BuildSystem {

    @Override
    public void create(@NotNull Module module, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        // TODO impl
    }

    @Override
    public void finishSetup(@NotNull Module module, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        // TODO impl
    }

    @Override
    public void reImport(@NotNull Module module, @NotNull PlatformType type) {
        // TODO impl
    }
}
