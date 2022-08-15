/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle

import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import java.io.File

data class McpModelData(
    val module: ModuleData,
    val settings: McpModuleSettings.State,
    val taskName: String?,
    val accessTransformers: List<File>?
) : AbstractExternalEntityData(module.owner) {
    companion object {
        // Process McpModelData after builtin services (e.g. dependency or module data)
        val KEY = Key.create(McpModelData::class.java, ProjectKeys.TASK.processingWeight + 1)
    }
}
