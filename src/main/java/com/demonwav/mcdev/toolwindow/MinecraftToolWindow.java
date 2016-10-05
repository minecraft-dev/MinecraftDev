package com.demonwav.mcdev.toolwindow;

import com.demonwav.mcdev.platform.MinecraftModule;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;

public class MinecraftToolWindow {
    private JPanel panel;

    private Project project;
    private Set<MinecraftModule> mcModules = new HashSet<>();

    public void setProjectAndInit(@NotNull Project project) {
        this.project = project;

        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            final MinecraftModule instance = MinecraftModule.getInstance(module);
            if (instance != null) {
                mcModules.add(instance);
            }
        }
    }

    public JPanel getPanel() {
        return panel;
    }
}
