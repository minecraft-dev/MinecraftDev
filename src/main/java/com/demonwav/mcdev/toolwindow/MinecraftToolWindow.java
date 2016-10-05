package com.demonwav.mcdev.toolwindow;

import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.OrderedSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import javax.swing.JPanel;

public class MinecraftToolWindow {
    private JPanel panel;

    private Project project;
    private OrderedSet<MinecraftModule> mcModules = new OrderedSet<>();

    public void setProjectAndInit(@NotNull Project project) {
        this.project = project;

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            final MinecraftModule instance = MinecraftModule.getInstance(module);
            if (instance != null) {
                mcModules.add(instance);
            }
        }

        Collections.sort(mcModules, (m1, m2) -> m1.getIdeaModule().getName().compareTo(m2.getIdeaModule().getName()));
    }

    public JPanel getPanel() {
        return panel;
    }
}
