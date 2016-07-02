package com.demonwav.mcdev.platform.liteloader;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.ProjectConfiguration;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class LiteLoaderProjectConfiguration extends ProjectConfiguration {

    public String mcpVersion;

    public LiteLoaderProjectConfiguration() {
        type = PlatformType.LITELOADER;
    }

    @Override
    public void create(@NotNull Project project, @NotNull BuildSystem buildSystem, @NotNull ProgressIndicator indicator) {

    }


}
