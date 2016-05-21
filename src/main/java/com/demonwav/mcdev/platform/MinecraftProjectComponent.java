package com.demonwav.mcdev.platform;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;

public class MinecraftProjectComponent extends AbstractProjectComponent {

    protected MinecraftProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();
        StartupManager.getInstance(myProject).registerPostStartupActivity(() -> {
            for (Module module : ModuleManager.getInstance(myProject).getModules()) {
                PlatformUtil.getInstance(module);
            }
        });
    }
}
