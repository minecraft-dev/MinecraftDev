/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */

package com.demonwav.mcdev.platform;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MinecraftProjectComponent extends AbstractProjectComponent {

    protected MinecraftProjectComponent(@NotNull Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();
        StartupManager.getInstance(myProject).registerPostStartupActivity(() -> {
            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                Optional.ofNullable(MinecraftModule.getInstance(module))
                    .ifPresent(m -> m.getTypes().forEach(t -> t.performCreationSettingSetup(myProject)));
            }
            MinecraftModule.doReadyActions();
        });
    }
}
