/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.util;

import org.jetbrains.annotations.NotNull;

public final class CanaryConstants {

    /* Hooks */
    @NotNull public static final String HOOK_HANDLER_ANNOTATION = "net.canarymod.hook.HookHandler";
    @NotNull public static final String HOOK_CLASS = "net.canarymod.hook.Hook";
    @NotNull public static final String LISTENER_CLASS = "net.canarymod.plugin.PluginListener";
    @NotNull public static final String PRIORITY_CLASS = "net.canarymod.plugin.Priority";

    /* Commands */
    @NotNull public static final String CHAT_FORMAT_CLASS = "net.canarymod.chat.ChatFormat";
    @NotNull public static final String COMMAND_ANNOTATION = "net.canarymod.commandsys.Command";
    @NotNull public static final String COMMAND_LISTENER_CLASS = "net.canarymod.commandsys.CommandListener";
    @NotNull public static final String LEGACY_COLORS_CLASS = "net.canarymod.chat.Colors";
    @NotNull public static final String LEGACY_TEXT_FORMAT_CLASS = "net.canarymod.chat.TextFormat";
    @NotNull public static final String MCP_CHAT_FORMATTING = "net.minecraft.util.EnumChatFormatting";
    @NotNull public static final String TAB_COMPLETE_ANNOTATION = "net.canarymod.commandsys.TabComplete";

    /* Database */
    @NotNull public static final String COLUMN_ANNOTATION = "net.canarymod.database.Column";

    private CanaryConstants() {
    }

}
