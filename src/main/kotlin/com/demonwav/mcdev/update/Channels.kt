/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.openapi.updateSettings.impl.UpdateSettings

enum class Channels(val title: String, val url: String) {
    NIGHTLY("Nightly", "https://plugins.jetbrains.com/plugins/Nightly/8327");

    fun hasChannel(): Boolean {
        return UpdateSettings.getInstance().pluginHosts.contains(url)
    }
}
