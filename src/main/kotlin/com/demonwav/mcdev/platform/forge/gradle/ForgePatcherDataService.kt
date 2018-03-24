/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.facet.MinecraftFacetConfiguration
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.findChildren
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project

class ForgePatcherDataService : AbstractProjectDataService<ForgePatcherModelData, Module>() {

    override fun getTargetDataKey() = ForgePatcherModelData.KEY

    override fun importData(toImport: Collection<DataNode<ForgePatcherModelData>>,
                            projectData: ProjectData?,
                            project: Project,
                            modelsProvider: IdeModifiableModelsProvider) {
        if (projectData == null || toImport.isEmpty()) {
            return
        }

        for (node in toImport) {
            val data = node.data

            val moduleManager = ModuleManager.getInstance(project)

            for (name in data.model.projects) {
                val rootModule = moduleManager.findModuleByName(name) ?: continue
                var children = rootModule.findChildren()

                if (children.isEmpty()) {
                    children = setOf(rootModule)
                }

                children.forEach { register(it, modelsProvider) }
                for (m in moduleManager.modules) {
                    if (m !in children) {
                        MinecraftFacet.getInstance(m)?.apply {
                            configuration.state.forgePatcher = false
                            refresh()
                        }
                    }
                }

                for (child in children) {
                    val mcpModule = MinecraftFacet.getInstance(child, McpModuleType) ?: continue
                    val mcp = data.model.mcpModel
                    mcpModule.updateSettings(McpModuleSettings.State(mcp.minecraftVersion, mcp.mcpVersion, mcp.mappingFiles))
                }
            }
        }
    }

    private fun register(module: Module, modelsProvider: IdeModifiableModelsProvider) {
        val model = modelsProvider.getModifiableFacetModel(module)
        val facet = model.getFacetByType(MinecraftFacet.ID)

        if (facet == null) {
            val configuration = MinecraftFacetConfiguration()
            configuration.state.autoDetectTypes.addAll(platforms)
            configuration.state.forgePatcher = true

            model.addFacet(MinecraftFacet.facetType.createFacet(module, "Minecraft", configuration, null))
        } else {
            val types = facet.configuration.state.autoDetectTypes
            facet.configuration.state.forgePatcher = true
            types.clear()
            types.addAll(platforms)
            facet.refresh()
        }
    }

    companion object {
        private val platforms = setOf(PlatformType.FORGE, PlatformType.MCP)
    }
}
