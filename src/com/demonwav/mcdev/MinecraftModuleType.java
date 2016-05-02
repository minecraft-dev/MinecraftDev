package com.demonwav.mcdev;

import com.demonwav.mcdev.creator.MinecraftModuleBuilder;
import com.demonwav.mcdev.icons.MinecraftProjectsIcons;

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
        return MinecraftProjectsIcons.MinecraftBig;
    }

    @Override
    public Icon getIcon() {
        return MinecraftProjectsIcons.Minecraft;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean isOpened) {
        return MinecraftProjectsIcons.Minecraft;
    }
}
