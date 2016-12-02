/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

data class MinecraftSettingsState(
    var isShowProjectPlatformIcons: Boolean = true,
    var isShowEventListenerGutterIcons: Boolean = true,
    var isShowChatColorGutterIcons: Boolean = true,
    var isShowChatColorUnderlines: Boolean = false,
    var isEnableSideOnlyChecks: Boolean = true,
    var underlineType: MinecraftSettings.UnderlineType = MinecraftSettings.UnderlineType.DOTTED
)
