package com.demonwav.mcdev.transition;

import com.demonwav.mcdev.platform.MinecraftModuleType;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;

import java.util.Objects;

public class TransitionProjectComponent extends AbstractProjectComponent {

    private final static String[] types = {
            "BUKKIT_MODULE_TYPE",
            "SPIGOT_MODULE_TYPE",
            "PAPER_MODULE_TYPE",
            "SPONGE_MODULE_TYPE",
            "FORGE_MODULE_TYPE",
            "BUNGEECORD_MODULE_TYPE"
    };

    protected TransitionProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();
        // Reset all Modules back to JavaModuleType
        for (Module module : ModuleManager.getInstance(myProject).getModules()) {
            for (String type : types) {
                if (Objects.equals(module.getOptionValue("type"), type)) {
                    module.setOption("type", JavaModuleType.getModuleType().getId());
                    MinecraftModuleType.setOption(module, type);
                }
            }
        }
    }


}
