/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.update

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

object PluginUtil {
    val PLUGIN_ID = PluginId.getId("com.demonwav.minecraft-dev")

    val plugin: IdeaPluginDescriptor
        get() {
            return PluginManagerCore.getPlugin(PLUGIN_ID)
                ?: error("Minecraft Development plugin not found: " + PluginManagerCore.getPlugins().contentToString())
        }

    val pluginVersion: String
        get() = plugin.version

    val useragent: String
        get() = "github_org/minecraft-dev/$pluginVersion"
}
