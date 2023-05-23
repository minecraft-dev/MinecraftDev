/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
        modelsProvider: IdeModifiableModelsProvider,
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
        }
    }
}
