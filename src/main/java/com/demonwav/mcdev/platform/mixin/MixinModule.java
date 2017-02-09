/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin;

import com.demonwav.mcdev.buildsystem.BuildSystem;
import com.demonwav.mcdev.platform.AbstractModule;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class MixinModule extends AbstractModule {

    MixinModule(@NotNull Module module) {
        super(module);
        this.buildSystem = BuildSystem.getInstance(module);
        if (buildSystem != null) {
            buildSystem.reImport(module);
        }
    }

    @Override
    public MixinModuleType getModuleType() {
        return MixinModuleType.getInstance();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.MIXIN;
    }

    @Override
    public Icon getIcon() {
        return null;
    }
}
