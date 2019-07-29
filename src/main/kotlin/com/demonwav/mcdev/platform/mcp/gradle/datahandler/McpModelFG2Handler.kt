/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.gradle.datahandler

import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.gradle.McpModelData
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG2
import com.demonwav.mcdev.platform.mcp.srg.SrgType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

object McpModelFG2Handler : McpModelDataHandler {

    override fun build(
        gradleModule: IdeaModule,
        module: ModuleData,
        resolverCtx: ProjectResolverContext
    ): McpModelData? {
        val data = resolverCtx.getExtraProject(gradleModule, McpModelFG2::class.java) ?: return null
        return McpModelData(
            module, McpModuleSettings.State(
                data.minecraftVersion,
                data.mcpVersion,
                data.mappingFiles.find { it.endsWith("mcp-srg.srg") },
                SrgType.SRG
            )
        )
    }
}
