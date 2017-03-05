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

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.creator.MinecraftModuleBuilder;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public class MinecraftModuleType extends JavaModuleType {

    @NotNull
    private static final String ID = "MINECRAFT_MODULE_TYPE";
    public static final String OPTION = "com.demonwav.mcdev.MinecraftModuleTypes"; // TODO remove

    @NotNull
    public static MinecraftModuleType getInstance() {
        return (MinecraftModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @NotNull
    @Override
    public MinecraftModuleBuilder createModuleBuilder() {
        return new MinecraftModuleBuilder();
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.MINECRAFT_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.MINECRAFT_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.MINECRAFT_ICON;
    }
}
