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
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class McpProjectResolverExtension : AbstractProjectResolverExtension() {

    // Register our custom Gradle tooling API model in IntelliJ's project resolver
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> = setOf(McpModel::class.java)

    // Adds the source of our model to the classpath of the Gradle build
    override fun getToolingExtensionsClasses(): Set<Class<out Any>> = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val model = resolverCtx.getExtraProject(gradleModule, McpModel::class.java)
        if (model != null) {
            val data = McpModelData(ideModule.data, McpModuleSettings.State(
                    model.minecraftVersion,
                    model.mcpVersion,
                    model.mappingFiles
            ))

            // Register our data in the module
            ideModule.createChild(McpModelData.KEY, data)
        }

        // Process the other resolver extensions
        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
