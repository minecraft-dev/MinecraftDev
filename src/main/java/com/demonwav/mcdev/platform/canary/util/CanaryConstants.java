package com.demonwav.mcdev.platform.canary.util;

import org.jetbrains.annotations.NotNull;

public final class CanaryConstants {

    @NotNull public static final String HANDLER_ANNOTATION = "net.canarymod.hook.HookHandler";
    @NotNull public static final String COMMAND_ANNOTATION = "net.canarymod.commandsys.Command";
    @NotNull public static final String TABCOMPLETE_ANNOTATION = "net.canarymod.commandsys.TabComplete";
    @NotNull public static final String COLUMN_ANNOTATION = "net.canarymod.database.Column";

    @NotNull public static final String CHAT_FORMAT_CLASS = "net.canarymod.chat.ChatFormat";
    @NotNull public static final String LEGACY_COLORS_CLASS = "net.canarymod.chat.Colors";
    @NotNull public static final String LEGACY_TEXT_FORMAT_CLASS = "net.canarymod.chat.TextFormat";
    @NotNull public static final String HOOK_CLASS = "net.canarymod.hook.Hook";

    private CanaryConstants() {
    }

}
