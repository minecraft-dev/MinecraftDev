package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.PlatformType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

import javax.swing.Icon;

public class SpigotModuleType extends BukkitModuleType {

    private static final String ID = "SPIGOT_MODULE_TYPE";
    private static final SpigotModuleType instance = new SpigotModuleType();

    private SpigotModuleType() {
        super(ID, "org.spigotmc", "spigot-api");
        BungeeCordModuleType.addBungeeColors(colorMap);
    }

    protected SpigotModuleType(final String ID, final String groupId, final String artifactId) {
        super(ID, groupId, artifactId);
        BungeeCordModuleType.addBungeeColors(colorMap);
    }

    public static SpigotModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.SPIGOT;
    }

    @Override
    public Icon getBigIcon() {
        return PlatformAssets.SPIGOT_ICON_2X;
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
