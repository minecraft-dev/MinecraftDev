package com.demonwav.mcdev;

import com.demonwav.mcdev.icons.MinecraftProjectsIcons;

import com.intellij.openapi.module.ModuleTypeManager;

import javax.swing.Icon;

public class PaperModuleType extends SpigotModuleType {

    private static final String ID = "PAPER_MODULE_TYPE";

    public PaperModuleType() {
        super(ID);
    }

    public static PaperModuleType getInstance() {
        return (PaperModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @Override
    public Icon getBigIcon() {
        return MinecraftProjectsIcons.PaperBig;
    }

    @Override
    public Icon getIcon() {
        return MinecraftProjectsIcons.Paper;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return MinecraftProjectsIcons.Paper;
    }
}
