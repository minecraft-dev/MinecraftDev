/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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

public class MinecraftProjectComponent extends AbstractProjectComponent {

    protected MinecraftProjectComponent(@NotNull Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();
        StartupManager.getInstance(myProject).registerPostStartupActivity(() -> {
            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                MinecraftModule.getInstance(module);
            }
            new Thread(() -> {
                // Wait 10 seconds (should be PLENTY of time)
                for (int i = 0; i < 1000; i++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // Clear out the ready actions
                MinecraftModule.cleanReadyActions();
            }).start();
        });
    }
}
