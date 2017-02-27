/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle

import com.demonwav.mcdev.buildsystem.gradle.AbstractDataService
import com.demonwav.mcdev.platform.MinecraftModule
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

class McpDataService : AbstractProjectDataService<McpModelData, Module>() {

    override fun getTargetDataKey(): Key<McpModelData> = McpModelData.KEY

    override fun importData(toImport: Collection<DataNode<McpModelData>>,
                            projectData: ProjectData?,
                            project: Project,
                            modelsProvider: IdeModifiableModelsProvider) {
        if (projectData == null || toImport.isEmpty()) {
            return
        }

        for (node in toImport) {
            val data = node.data
            val module = modelsProvider.findIdeModule(data.module) ?: continue

            AbstractDataService.addToFacetState(module, McpModuleType)
            MinecraftModuleType.addOption(module, McpModuleType.id)
            val mcpModule = MinecraftModule.getInstance(module, McpModuleType) ?: continue

            // Update the local settings and recompute the SRG map
            mcpModule.updateSettings(data.settings)
        }
    }
}
