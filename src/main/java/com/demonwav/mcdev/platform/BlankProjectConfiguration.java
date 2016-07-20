package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BlankProjectConfiguration extends ProjectConfiguration {
    @Override
    public void create(@NotNull Project project, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator) {
    }
}
