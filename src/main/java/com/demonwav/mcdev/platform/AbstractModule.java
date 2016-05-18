package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.intellij.openapi.module.Module;

import javax.swing.Icon;

public abstract class AbstractModule {
    protected Module module;
    protected BuildSystem buildSystem;

    public Module getModule() {
        return module;
    }

    public BuildSystem getBuildSystem() {
        return buildSystem;
    }

    public abstract PlatformType getType();
    public abstract Icon getIcon();
}
