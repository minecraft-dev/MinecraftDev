package com.demonwav.mcdev;

import com.demonwav.mcdev.icons.MinecraftProjectsIcons;

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
        return MinecraftProjectsIcons.BungeeCordBig;
    }

    @Override
    public Icon getIcon() {
        return MinecraftProjectsIcons.BungeeCord;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return MinecraftProjectsIcons.BungeeCord;
    }
}
