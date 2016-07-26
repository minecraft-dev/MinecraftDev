package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class PaperModuleType extends SpigotModuleType {

    private static final String ID = "PAPER_MODULE_TYPE";
    private static final PaperModuleType instance = new PaperModuleType();

    private PaperModuleType() {
        super("com.destroystokyo.paper", "paper-api");
    }

    @NotNull
    public static PaperModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.PAPER;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.PAPER_ICON;
    }

    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public BukkitModule generateModule(Module module) {
        return new BukkitModule<>(module, this);
    }
}
