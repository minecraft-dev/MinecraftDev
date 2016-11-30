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

class MinecraftSettingsState {

    var isShowProjectPlatformIcons = true
    var isShowEventListenerGutterIcons = true
    var isShowChatColorGutterIcons = true
    var isShowChatColorUnderlines = false

    var isEnableSideOnlyChecks = true

    var underlineType: MinecraftSettings.UnderlineType = MinecraftSettings.UnderlineType.DOTTED
}
