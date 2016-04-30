package com.demonwav.mcdev;

import com.demonwav.mcdev.icons.BukkitProjectsIcons;

import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class SpigotModuleType extends JavaModuleType {

    private static final String ID = "SPIGOT_MODULE_TYPE";

    public SpigotModuleType() {
        super(ID);
    }

    public static SpigotModuleType getInstance() {
        return (SpigotModuleType) ModuleTypeManager.getInstance().findByID(ID);
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
