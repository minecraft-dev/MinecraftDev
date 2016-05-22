package com.demonwav.mcdev.platform.forge;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class ForgeModuleType extends MinecraftModuleType {

    private static final String ID = "FORGE_MODULE_TYPE";

    public ForgeModuleType() {
        super(ID, "net.minecraftforge.gradle", "ForgeGradle");
    }

    public static ForgeModuleType getInstance() {
        return (ForgeModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.FORGE;
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.FORGE_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.FORGE_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.FORGE_ICON;
    }
}
