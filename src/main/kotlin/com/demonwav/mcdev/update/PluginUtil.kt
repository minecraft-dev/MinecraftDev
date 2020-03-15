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

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object PluginUtil {
    val PLUGIN_ID = PluginId.getId("com.demonwav.minecraft-dev")

    val pluginVersion: String
        get() {
            val plugin = PluginManager.getPlugin(PLUGIN_ID)
                ?: error("Minecraft Development plugin not found: " + PluginManagerCore.getPlugins().contentToString())
            return plugin.version
        }
}
