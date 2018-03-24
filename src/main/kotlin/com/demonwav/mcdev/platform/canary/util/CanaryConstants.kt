/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.canary.util

object CanaryConstants {

    const val CANARY_INF = "Canary.inf"
    const val NEPTUNE_INF = "Neptune.inf"

    /* Hooks */
    const val HOOK_HANDLER_ANNOTATION = "net.canarymod.hook.HookHandler"
    const val HOOK_CLASS = "net.canarymod.hook.Hook"
    const val LISTENER_CLASS = "net.canarymod.plugin.PluginListener"
    const val PRIORITY_CLASS = "net.canarymod.plugin.Priority"

    /* Commands */
    const val CHAT_FORMAT_CLASS = "net.canarymod.chat.ChatFormat"
    const val COMMAND_ANNOTATION = "net.canarymod.commandsys.Command"
    const val COMMAND_LISTENER_CLASS = "net.canarymod.commandsys.CommandListener"
    const val LEGACY_COLORS_CLASS = "net.canarymod.chat.Colors"
    const val LEGACY_TEXT_FORMAT_CLASS = "net.canarymod.chat.TextFormat"
    const val MCP_CHAT_FORMATTING = "net.minecraft.util.EnumChatFormatting"
    const val TAB_COMPLETE_ANNOTATION = "net.canarymod.commandsys.TabComplete"

    /* Database */
    const val COLUMN_ANNOTATION = "net.canarymod.database.Column"

}
