package com.demonwav;

import com.demonwav.icons.BukkitProjectsIcons;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class BungeeCordModuleType extends JavaModuleType {

    private static final String ID = "BUNGEECORD_MODULE_TYPE";

    public BungeeCordModuleType() {
        super(ID);
    }

    public static BungeeCordModuleType getInstance() {
        return (BungeeCordModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public Icon getBigIcon() {
        return BukkitProjectsIcons.SpigotProjectBig;
    }

    @Override
    public Icon getIcon() {
        return BukkitProjectsIcons.SpigotProject;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return BukkitProjectsIcons.SpigotProject;
    }
}
