/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.module.Module

@State(name = "McpModuleSettings", storages = arrayOf(Storage(StoragePathMacros.MODULE_FILE)))
class McpModuleSettings : PersistentStateComponent<McpModuleSettings.State> {

    data class State(
        var minecraftVersion: String? = null,
        var mcpVersion: String? = null,
        var mappingFiles: Set<String> = mutableSetOf()
    )

    private var state: State = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        @JvmStatic fun getInstance(module: Module) =
            // Normally this should use the ServiceManager but that doesn't support getting a service for a module
            // This is based on ServiceManager.doGetService with the module as component manager
            module.picoContainer.getComponentInstanceOfType(McpModuleSettings::class.java) as McpModuleSettings
    }

}
