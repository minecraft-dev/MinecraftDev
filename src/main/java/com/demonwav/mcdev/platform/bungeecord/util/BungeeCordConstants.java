/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.platform.bungeecord.util;

import org.jetbrains.annotations.NotNull;

public class BungeeCordConstants {

    @NotNull public static final String HANDLER_ANNOTATION = "net.md_5.bungee.event.EventHandler";
    @NotNull public static final String EVENT_PRIORITY_CLASS = "net.md_5.bungee.event.EventPriority";
    @NotNull public static final String LISTENER_CLASS = "net.md_5.bungee.api.plugin.Listener";
    @NotNull public static final String CHAT_COLOR_CLASS = "net.md_5.bungee.api.ChatColor";
    @NotNull public static final String EVENT_CLASS = "net.md_5.bungee.api.plugin.Event";
    @NotNull public static final String PLUGIN = "net.md_5.bungee.api.plugin.Plugin";

    private BungeeCordConstants() {
    }
}
