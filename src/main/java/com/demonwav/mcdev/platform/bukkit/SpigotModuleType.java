package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.util.CommonColors;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class SpigotModuleType extends BukkitModuleType {

    private static final String ID = "SPIGOT_MODULE_TYPE";
    private static final SpigotModuleType instance = new SpigotModuleType();

    private SpigotModuleType() {
        super("org.spigotmc", "spigot-api");
        CommonColors.applyStandardColors(this.colorMap, "net.md_5.bungee.api.ChatColor");
    }

    protected SpigotModuleType(final String groupId, final String artifactId) {
        super(groupId, artifactId);
        CommonColors.applyStandardColors(this.colorMap, "net.md_5.bungee.api.ChatColor");
    }

    @NotNull
    public static SpigotModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.SPIGOT;
    }

    @Override
    public Icon getIcon() {
        return PlatformAssets.SPIGOT_ICON;
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
