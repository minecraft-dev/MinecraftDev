package com.demonwav.mcdev.platform.bukkit.util;

import org.jetbrains.annotations.NotNull;

public class BukkitConstants {

    @NotNull public static final String BUKKIT_HANDLER_ANNOTATION = "org.bukkit.event.EventHandler";
    @NotNull public static final String BUKKIT_EVENT_PRIORITY_CLASS = "org.bukkit.event.EventPriority";
    @NotNull public static final String BUKKIT_LISTENER_CLASS = "org.bukkit.event.Listener";
    @NotNull public static final String BUKKIT_CHAT_COLOR_CLASS = "org.bukkit.ChatColor";
    @NotNull public static final String BUKKIT_EVENT_CLASS = "org.bukkit.event.Event";
    @NotNull public static final String JAVA_PLUGIN = "org.bukkit.plugin.java.JavaPlugin";
    @NotNull public static final String BUKKIT_EVENT_ISCANCELLED_METHOD_NAME = "isCancelled";
    @NotNull public static final String BUKKIT_CANCELLABLE_CLASS = "org.bukkit.event.Cancellable";

    private BukkitConstants() {
    }
}
