package com.demonwav.mcdev.platform.bungeecord;

import com.demonwav.mcdev.asset.PlatformAssets;
import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.PlatformType;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;

public class BungeeCordModuleType extends AbstractModuleType<BungeeCordModule> {

    private static final String ID = "BUNGEECORD_MODULE_TYPE";
    private static final BungeeCordModuleType instance = new BungeeCordModuleType();

    private final LinkedHashMap<String, Color> colorMap = new LinkedHashMap<>();

    private BungeeCordModuleType() {
        super("net.md-5", "bungeecord-api");
        BungeeCordModuleType.addBungeeColors(colorMap);
    }

    public static BungeeCordModuleType getInstance() {
        return instance;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BUNGEECORD;
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
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public List<String> getIgnoredAnnotations() {
        return ImmutableList.of("net.md_5.bungee.event.EventHandler");
    }

    @NotNull
    @Override
    public List<String> getListenerAnnotations() {
        return ImmutableList.of("net.md_5.bungee.event.EventHandler");
    }

    @NotNull
    @Override
    public Map<String, Color> getClassToColorMappings() {
        return colorMap;
    }

    @NotNull
    @Override
    public BungeeCordModule generateModule(Module module) {
        return new BungeeCordModule(module);
    }

    public static void addBungeeColors(@NotNull Map<String, Color> colorMap) {
        colorMap.put("net.md_5.bungee.api.ChatColor.DARK_RED", new Color(0xAA0000));
        colorMap.put("net.md_5.bungee.api.ChatColor.RED", new Color(0xFF5555));
        colorMap.put("net.md_5.bungee.api.ChatColor.GOLD", new Color(0xFFAA00));
        colorMap.put("net.md_5.bungee.api.ChatColor.YELLOW", new Color(0xFFFF55));
        colorMap.put("net.md_5.bungee.api.ChatColor.DARK_GREEN", new Color(0x00AA00));
        colorMap.put("net.md_5.bungee.api.ChatColor.GREEN", new Color(0x55FF55));
        colorMap.put("net.md_5.bungee.api.ChatColor.AQUA", new Color(0x55FFFF));
        colorMap.put("net.md_5.bungee.api.ChatColor.DARK_AQUA", new Color(0x00AAAA));
        colorMap.put("net.md_5.bungee.api.ChatColor.DARK_BLUE", new Color(0x0000AA));
        colorMap.put("net.md_5.bungee.api.ChatColor.BLUE", new Color(0x5555FF));
        colorMap.put("net.md_5.bungee.api.ChatColor.LIGHT_PURPLE", new Color(0xFF55FF));
        colorMap.put("net.md_5.bungee.api.ChatColor.DARK_PURPLE", new Color(0xAA00AA));
        colorMap.put("net.md_5.bungee.api.ChatColor.WHITE", new Color(0xFFFFFF));
        colorMap.put("net.md_5.bungee.api.ChatColor.GRAY", new Color(0xAAAAAA));
        colorMap.put("net.md_5.bungee.api.ChatColor.DARK_GRAY", new Color(0x555555));
        colorMap.put("net.md_5.bungee.api.ChatColor.BLACK", new Color(0x000000));
    }
}
