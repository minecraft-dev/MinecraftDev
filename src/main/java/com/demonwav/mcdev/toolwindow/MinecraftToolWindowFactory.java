/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class MinecraftToolWindowFactory implements ToolWindowFactory {
    private final MinecraftToolWindow mcToolWindow = new MinecraftToolWindow();

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        mcToolWindow.setProjectAndInit(project);
        toolWindow.getComponent().add(mcToolWindow.getPanel());
    }
}
