/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.datahandler

import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.gradle.McpModelData
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG2
import com.demonwav.mcdev.platform.mcp.srg.SrgType
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

object McpModelFG2Handler : McpModelDataHandler {

    override fun build(
        gradleModule: IdeaModule,
        node: DataNode<ModuleData>,
        resolverCtx: ProjectResolverContext
    ) {
        val data = resolverCtx.getExtraProject(gradleModule, McpModelFG2::class.java) ?: return

        val state = McpModuleSettings.State(
            data.minecraftVersion,
            data.mcpVersion,
            data.mappingFiles.find { it.endsWith("mcp-srg.srg") },
            SrgType.SRG
        )

        val modelData = McpModelData(
            node.data,
            state,
            null,
            null
        )

        node.createChild(
            McpModelData.KEY,
            McpModelData(
                node.data,
                McpModuleSettings.State(
                    data.minecraftVersion,
                    data.mcpVersion,
                    data.mappingFiles.find { it.endsWith("mcp-srg.srg") },
                    SrgType.SRG
                ),
                null,
                null
            )
        )

        for (child in node.children) {
            val childData = child.data
            if (childData is GradleSourceSetData) {
                child.createChild(McpModelData.KEY, modelData.copy(module = childData))
            }
        }
    }
}
