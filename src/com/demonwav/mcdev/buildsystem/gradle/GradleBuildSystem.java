package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class GradleBuildSystem extends BuildSystem {

    @Override
    public void create(@NotNull Project project, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        // TODO impl
    }

    @Override
    public void finishSetup(@NotNull Project project, @NotNull PlatformType type, @NotNull ProjectConfiguration configuration) {
        // TODO impl
    }

    @Override
    public BuildSystem reImport(@NotNull Project project, @NotNull PlatformType type) {
        return null;
    }
}
