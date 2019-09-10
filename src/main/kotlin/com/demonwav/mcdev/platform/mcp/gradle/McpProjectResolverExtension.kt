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

import com.demonwav.mcdev.platform.mcp.gradle.datahandler.McpModelFG2Handler
import com.demonwav.mcdev.platform.mcp.gradle.datahandler.McpModelFG3Handler
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG2
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG3
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class McpProjectResolverExtension : AbstractProjectResolverExtension() {

    // Register our custom Gradle tooling API model in IntelliJ's project resolver
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> =
        setOf(McpModelFG2::class.java, McpModelFG3::class.java)

    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        var data: McpModelData? = null
        for (handler in handlers) {
            data = handler.build(gradleModule, ideModule.data, resolverCtx)
            if (data != null) {
                break
            }
        }

        data?.let {
            // Register our data in the module
            ideModule.createChild(McpModelData.KEY, data)
        }

        // Process the other resolver extensions
        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    companion object {
        private val handlers = listOf(McpModelFG2Handler, McpModelFG3Handler)
    }
}
