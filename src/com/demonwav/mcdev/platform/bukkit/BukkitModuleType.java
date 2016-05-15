package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class BukkitModuleType extends MinecraftModuleType {

    private static final String ID = "BUKKIT_MODULE_TYPE";

    public BukkitModuleType() {
        super(ID, "org.bukkit", "bukkit");
    }

    public BukkitModuleType(final String ID, final String groupId, final String artifactId) {
        super(ID, groupId, artifactId);
    }

    public static BukkitModuleType getInstance() {
        return (BukkitModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.BUKKIT_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.BUKKIT_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.BUKKIT_ICON;
    }
}
