package com.demonwav.mcdev.asset;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public final class PlatformAssets {

    public static final Icon MINECRAFT_ICON = loadIcon("/assets/platform/icons/Minecraft.png");
    public static final Icon MINECRAFT_ICON_2X = loadIcon("/assets/platform/icons/Minecraft@2x.png");

    public static final Icon BUKKIT_ICON = loadIcon("/assets/platform/icons/Bukkit.png");
    public static final Icon BUKKIT_ICON_2X = loadIcon("/assets/platform/icons/Bukkit@2x.png");
    public static final Icon SPIGOT_ICON = loadIcon("/assets/platform/icons/Spigot.png");
    public static final Icon SPIGOT_ICON_2X = loadIcon("/assets/platform/icons/Spigot@2x.png");
    public static final Icon PAPER_ICON = loadIcon("/assets/platform/icons/Paper.png");
    public static final Icon PAPER_ICON_2X = loadIcon("/assets/platform/icons/Paper@2x.png");

    public static final Icon FORGE_ICON = loadIcon("/assets/platform/icons/Forge.png");
    public static final Icon FORGE_ICON_2X = loadIcon("/assets/platform/icons/Forge@2x.png");

    public static final Icon SPONGE_ICON = loadIcon("/assets/platform/icons/Sponge.png");
    public static final Icon SPONGE_ICON_2X = loadIcon("/assets/platform/icons/Sponge@2x.png");
    public static final Icon SPONGE_ICON_DARK = loadIcon("/assets/platform/icons/SpongeDark.png");
    public static final Icon SPONGE_ICON_DARK_2X = loadIcon("/assets/platform/icons/SpongeDark@2x.png");

    public static final Icon SPONGE_FORGE_ICON = loadIcon("/assets/platform/icons/SpongeForge.png");
    public static final Icon SPONGE_FORGE_ICON_2X = loadIcon("/assets/platform/icons/SpongeForge@2x.png");
    public static final Icon SPONGE_FORGE_ICON_DARK = loadIcon("/assets/platform/icons/SpongeForgeDark.png");
    public static final Icon SPONGE_FORGE_ICON_DARK_2X = loadIcon("/assets/platform/icons/SpongeForgeDark@2x.png");

    public static final Icon BUNGEECORD_ICON = loadIcon("/assets/platform/icons/BungeeCord.png");
    public static final Icon BUNGEECORD_ICON_2X = loadIcon("/assets/platform/icons/BungeeCord@2x.png");

    public static final Icon LITELOADER_ICON = loadIcon("/assets/platform/icons/LiteLoader.png");
    public static final Icon LITELOADER_ICON_2X = loadIcon("/assets/platform/icons/LiteLoader@2x.png");

    public static final Icon LISTENER = loadIcon("/assets/platform/icons/listener/EventListener_dark.png");

    private PlatformAssets() {
    }

    private static Icon loadIcon(String path) {
        return IconLoader.getIcon(path, PlatformAssets.class);
    }
}
