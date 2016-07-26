package com.demonwav.mcdev.asset;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

@SuppressWarnings("unused")
public final class PlatformAssets {

    @NotNull public static final Icon MINECRAFT_ICON = loadIcon("/assets/platform/icons/Minecraft.png");
    @NotNull public static final Icon MINECRAFT_ICON_2X = loadIcon("/assets/platform/icons/Minecraft@2x.png");

    @NotNull public static final Icon BUKKIT_ICON = loadIcon("/assets/platform/icons/Bukkit.png");
    @NotNull public static final Icon BUKKIT_ICON_2X = loadIcon("/assets/platform/icons/Bukkit@2x.png");
    @NotNull public static final Icon SPIGOT_ICON = loadIcon("/assets/platform/icons/Spigot.png");
    @NotNull public static final Icon SPIGOT_ICON_2X = loadIcon("/assets/platform/icons/Spigot@2x.png");
    @NotNull public static final Icon PAPER_ICON = loadIcon("/assets/platform/icons/Paper.png");
    @NotNull public static final Icon PAPER_ICON_2X = loadIcon("/assets/platform/icons/Paper@2x.png");

    @NotNull public static final Icon FORGE_ICON = loadIcon("/assets/platform/icons/Forge.png");
    @NotNull public static final Icon FORGE_ICON_2X = loadIcon("/assets/platform/icons/Forge@2x.png");

    @NotNull public static final Icon SPONGE_ICON = loadIcon("/assets/platform/icons/Sponge.png");
    @NotNull public static final Icon SPONGE_ICON_2X = loadIcon("/assets/platform/icons/Sponge@2x.png");
    @NotNull public static final Icon SPONGE_ICON_DARK = loadIcon("/assets/platform/icons/SpongeDark.png");
    @NotNull public static final Icon SPONGE_ICON_DARK_2X = loadIcon("/assets/platform/icons/SpongeDark@2x.png");

    @NotNull public static final Icon SPONGE_FORGE_ICON = loadIcon("/assets/platform/icons/SpongeForge.png");
    @NotNull public static final Icon SPONGE_FORGE_ICON_2X = loadIcon("/assets/platform/icons/SpongeForge@2x.png");
    @NotNull public static final Icon SPONGE_FORGE_ICON_DARK = loadIcon("/assets/platform/icons/SpongeForgeDark.png");
    @NotNull public static final Icon SPONGE_FORGE_ICON_DARK_2X = loadIcon("/assets/platform/icons/SpongeForgeDark@2x.png");

    @NotNull public static final Icon BUNGEECORD_ICON = loadIcon("/assets/platform/icons/BungeeCord.png");
    @NotNull public static final Icon BUNGEECORD_ICON_2X = loadIcon("/assets/platform/icons/BungeeCord@2x.png");

    @NotNull public static final Icon LITELOADER_ICON = loadIcon("/assets/platform/icons/LiteLoader.png");
    @NotNull public static final Icon LITELOADER_ICON_2X = loadIcon("/assets/platform/icons/LiteLoader@2x.png");

    @NotNull public static final Icon LISTENER = loadIcon("/assets/platform/icons/listener/EventListener_dark.png");
    @NotNull public static final Icon PLUGIN = loadIcon("/assets/platform/icons/plugin.png");

    @NotNull public static final Icon MIXIN = loadIcon("/assets/platform/icons/mixin/mixin.png");
    @NotNull public static final Icon MIXIN_DARK = loadIcon("/assets/platform/icons/mixin/mixin_dark.png");

    private PlatformAssets() {
    }

    private static Icon loadIcon(String path) {
        return IconLoader.getIcon(path, PlatformAssets.class);
    }
}
