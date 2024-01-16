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

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.platform.mcp.srg.SrgType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager

@State(name = "McpModuleSettings", storages = [Storage(StoragePathMacros.MODULE_FILE)])
class McpModuleSettings : PersistentStateComponent<McpModuleSettings.State> {

    data class State(
        var minecraftVersion: String? = null,
        var mcpVersion: String? = null,
        var mappingFile: String? = null,
        var srgType: SrgType? = null,
        var platformVersion: String? = null,
    )

    private var state: State = State(srgType = SrgType.SRG)

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(module: Module): McpModuleSettings {
            // TODO: Migrate these to the facet's settings

            val settings = module.getService(McpModuleSettings::class.java) as McpModuleSettings
            if (settings.getState().minecraftVersion != null) {
                return settings
            }

            // Attempt to find settings on the parent module
            val manager = ModuleManager.getInstance(module.project)
            val path = manager.getModuleGroupPath(module) ?: return settings
            val parent = manager.findModuleByName(path.last()) ?: return settings

            val newSettings = parent.getService(McpModuleSettings::class.java) as McpModuleSettings
            if (newSettings.getState().minecraftVersion == null) {
                return settings
            }
            return newSettings
        }
    }
}
