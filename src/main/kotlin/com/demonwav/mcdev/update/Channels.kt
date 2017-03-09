/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.update

import com.intellij.openapi.updateSettings.impl.UpdateSettings

enum class Channels(val title: String, val url: String, val index: Int) {
    ;

    fun hasChannel(): Boolean {
        return UpdateSettings.getInstance().pluginHosts.contains(url)
    }

    companion object {
        fun getChannel(index: Int) = values().firstOrNull { it.index == index }
        fun orderedList() = listOf<Channels>()
    }
}
