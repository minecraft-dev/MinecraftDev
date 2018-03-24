/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle

import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData

class McpModelData(val module: ModuleData, val settings: McpModuleSettings.State) : AbstractExternalEntityData(module.owner) {

    companion object {
        // Process McpModelData after builtin services (e.g. dependency or module data)
        val KEY = Key.create(McpModelData::class.java, ProjectKeys.TASK.processingWeight + 1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is McpModelData) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }

        if (module != other.module) {
            return false
        }
        if (settings != other.settings) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + module.hashCode()
        result = 31 * result + settings.hashCode()
        return result
    }
}
