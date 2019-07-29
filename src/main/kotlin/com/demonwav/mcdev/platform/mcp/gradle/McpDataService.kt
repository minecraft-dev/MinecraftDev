/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle

import com.demonwav.mcdev.facet.MinecraftFacet
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

    override fun importData(
        toImport: Collection<DataNode<McpModelData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        if (projectData == null || toImport.isEmpty()) {
            return
        }

        for (node in toImport) {
            val data = node.data
            val module = modelsProvider.findIdeModule(data.module) ?: continue

            val mcpModule = MinecraftFacet.getInstance(module, McpModuleType)
            if (mcpModule != null) {
                // Update the local settings and recompute the SRG map
                mcpModule.updateSettings(data.settings)
                continue
            }

            val children = MinecraftFacet.getChildInstances(module)
            for (child in children) {
                child.getModuleOfType(McpModuleType)?.updateSettings(data.settings)
            }
        }
    }
}
