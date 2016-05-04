package com.demonwav.mcdev.platform;

import com.demonwav.mcdev.creator.MinecraftModuleBuilder;
import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class MinecraftModuleType extends JavaModuleType {

    private static final String ID = "MINECRAFT_MODULE_TYPE";

    public MinecraftModuleType() {
        super(ID);
    }

    public MinecraftModuleType(String ID) {
        super(ID);
    }

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
