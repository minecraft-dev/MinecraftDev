/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.bukkit.util;

import org.jetbrains.annotations.NotNull;

public class BukkitConstants {

    @NotNull public static final String HANDLER_ANNOTATION = "org.bukkit.event.EventHandler";
    @NotNull public static final String EVENT_PRIORITY_CLASS = "org.bukkit.event.EventPriority";
    @NotNull public static final String LISTENER_CLASS = "org.bukkit.event.Listener";
    @NotNull public static final String CHAT_COLOR_CLASS = "org.bukkit.ChatColor";
    @NotNull public static final String EVENT_CLASS = "org.bukkit.event.Event";
    @NotNull public static final String JAVA_PLUGIN = "org.bukkit.plugin.java.JavaPlugin";
    @NotNull public static final String EVENT_ISCANCELLED_METHOD_NAME = "isCancelled";
    @NotNull public static final String CANCELLABLE_CLASS = "org.bukkit.event.Cancellable";

    private BukkitConstants() {
    }
}
