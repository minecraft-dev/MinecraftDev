package com.demonwav.mcdev.platform.bukkit;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public class BukkitModuleType extends AbstractModuleType<BukkitModule<?>> {

    private static final String ID = "BUKKIT_MODULE_TYPE";
    private static final BukkitModuleType instance = new BukkitModuleType();

    protected final HashMap<String, Color> colorMap = new HashMap<>();

    private BukkitModuleType() {
        super("org.bukkit", "bukkit");
        setupMap();
    }

    protected BukkitModuleType(final String ID, final String groupId, final String artifactId) {
        super(groupId, artifactId);
        setupMap();
    }

    private void setupMap() {
        // So we only ever have one instance of the map and we don't keep regenerating it
        colorMap.put("org.bukkit.ChatColor.AQUA", new Color(0x55FFFF));
        colorMap.put("org.bukkit.ChatColor.BLACK", new Color(0x000000));
        colorMap.put("org.bukkit.ChatColor.BLUE", new Color(0x5555FF));
        colorMap.put("org.bukkit.ChatColor.DARK_AQUA", new Color(0x00AAAA));
        colorMap.put("org.bukkit.ChatColor.DARK_BLUE", new Color(0x0000AA));
        colorMap.put("org.bukkit.ChatColor.DARK_GRAY", new Color(0x555555));
        colorMap.put("org.bukkit.ChatColor.DARK_GREEN", new Color(0x00AA00));
        colorMap.put("org.bukkit.ChatColor.DARK_PURPLE", new Color(0xAA00AA));
        colorMap.put("org.bukkit.ChatColor.DARK_RED", new Color(0xAA0000));
        colorMap.put("org.bukkit.ChatColor.GOLD", new Color(0xFFAA00));
        colorMap.put("org.bukkit.ChatColor.GRAY", new Color(0xAAAAAA));
        colorMap.put("org.bukkit.ChatColor.GREEN", new Color(0x55FF55));
        colorMap.put("org.bukkit.ChatColor.LIGHT_PURPLE", new Color(0xFF55FF));
        colorMap.put("org.bukkit.ChatColor.RED", new Color(0xFF5555));
        colorMap.put("org.bukkit.ChatColor.WHITE", new Color(0xFFFFFF));
        colorMap.put("org.bukkit.ChatColor.YELLOW", new Color(0xFFFF55));
    }

    public static BukkitModuleType getInstance() {
        return instance;
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

    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of("org.bukkit.event.EventHandler");
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of("org.bukkit.event.EventHandler");
    }

    @NotNull
    @Override
    public Map<String, Color> getClassToColorMappings() {
        return colorMap;
    }

    @NotNull
    @Override
    public BukkitModule generateModule(Module module) {
        return new BukkitModule<>(module, this);
    }
}
