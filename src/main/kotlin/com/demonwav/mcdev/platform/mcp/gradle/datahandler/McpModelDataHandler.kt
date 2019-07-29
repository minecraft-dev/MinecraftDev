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

import com.demonwav.mcdev.platform.mcp.gradle.McpModelData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

interface McpModelDataHandler {
    fun build(gradleModule: IdeaModule, module: ModuleData, resolverCtx: ProjectResolverContext): McpModelData?
}
