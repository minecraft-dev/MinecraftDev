package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.platform.PlatformUtil;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;

public class BukkitProjectComponent extends AbstractProjectComponent {

    protected BukkitProjectComponent(Project project) {
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
