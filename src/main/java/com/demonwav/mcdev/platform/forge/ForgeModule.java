package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class ForgeModule extends AbstractModule {

    ForgeModule(@NotNull Module module) {
        this.module = module;
        buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module, PlatformType.FORGE);
        }
    }

    @Override
    public ForgeModuleType getModuleType() {
        return ForgeModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.FORGE;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
    }
}
