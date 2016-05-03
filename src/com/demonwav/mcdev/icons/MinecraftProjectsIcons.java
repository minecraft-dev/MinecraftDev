package com.demonwav.mcdev.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public class MinecraftProjectsIcons {

    private static Icon load(String path) {
        return IconLoader.getIcon(path, MinecraftProjectsIcons.class);
    }

    public static final Icon Bukkit = load("/icons/Bukkit.png");
    public static final Icon BukkitBig = load("/icons/Bukkit@2x.png");
    public static final Icon Spigot = load("/icons/Spigot.png");
    public static final Icon SpigotBig = load("/icons/Spigot@2x.png");
    public static final Icon Minecraft = load("/icons/Minecraft.png");
    public static final Icon MinecraftBig = load("/icons/Minecraft@2x.png");
    public static final Icon Sponge = load("/icons/Sponge.png");
    public static final Icon SpongeBig = load("/icons/Sponge@2x.png");
    public static final Icon Paper = load("/icons/Paper.png");
    public static final Icon PaperBig = load("/icons/Paper@2x.png");
    public static final Icon BungeeCord = load("/icons/BungeeCord.png");
    public static final Icon BungeeCordBig = load("/icons/BungeeCord@2x.png");
    public static final Icon Forge = load("/icons/Forge.png");
    public static final Icon ForgeBig = load("/icons/Forge@2x.png");
}
