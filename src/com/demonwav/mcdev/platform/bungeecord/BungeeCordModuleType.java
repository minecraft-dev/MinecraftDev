package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.asset.PlatformAssets;

import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class BungeeCordModuleType extends MinecraftModuleType {

    private static final String ID = "BUNGEECORD_MODULE_TYPE";

    public BungeeCordModuleType() {
        super(ID);
    }

    public static BungeeCordModuleType getInstance() {
        return (BungeeCordModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.BUNGEECORD_ICON_2X;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.BUNGEECORD_ICON;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return PlatformAssets.BUNGEECORD_ICON;
    }
}
