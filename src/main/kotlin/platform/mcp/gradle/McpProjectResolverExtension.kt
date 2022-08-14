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

import com.demonwav.mcdev.platform.mcp.gradle.datahandler.McpModelFG2Handler
import com.demonwav.mcdev.platform.mcp.gradle.datahandler.McpModelFG3Handler
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG2
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG3
import com.demonwav.mcdev.util.runGradleTask
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import java.nio.file.Paths
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class McpProjectResolverExtension : AbstractProjectResolverExtension() {

    // Register our custom Gradle tooling API model in IntelliJ's project resolver
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> =
        setOf(McpModelFG2::class.java, McpModelFG3::class.java)

    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun resolveFinished(projectDataNode: DataNode<ProjectData>) {
        // FG3 requires us to run a task for each module
        // We do it here so that we can do this one time for all modules

        // We do need to have a project here though
        val project = resolverCtx.externalSystemTaskId.findProject() ?: return

        val allTaskNames = findAllTaskNames(projectDataNode)
        if (allTaskNames.isEmpty()) {
            // Seems to not be FG3
            return
        }

        val projectDirPath = Paths.get(projectDataNode.data.linkedExternalProjectPath)
        runGradleTask(project, projectDirPath) { settings ->
            settings.taskNames = allTaskNames
        }

        super.resolveFinished(projectDataNode)
    }

    private fun findAllTaskNames(node: DataNode<*>): List<String> {
        fun findAllTaskNames(node: DataNode<*>, taskNames: MutableList<String>) {
            val data = node.data
            if (data is McpModelData) {
                data.taskName?.let { taskName ->
                    taskNames += taskName
                }
            }
            for (child in node.children) {
                findAllTaskNames(child, taskNames)
            }
        }

        val res = arrayListOf<String>()
        findAllTaskNames(node, res)
        return res
    }

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        for (handler in handlers) {
            handler.build(gradleModule, ideModule, resolverCtx)
        }

        // Process the other resolver extensions
        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    companion object {
        private val handlers = listOf(McpModelFG2Handler, McpModelFG3Handler)
    }
}
