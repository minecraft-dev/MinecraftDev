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

import com.demonwav.mcdev.buildsystem.gradle.runGradleTask
import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.gradle.McpModelData
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG3
import com.demonwav.mcdev.platform.mcp.srg.SrgType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.ProjectManager
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

object McpModelFG3Handler : McpModelDataHandler {
    override fun build(
        gradleModule: IdeaModule,
        module: ModuleData,
        resolverCtx: ProjectResolverContext
    ): McpModelData? {
        val data = resolverCtx.getExtraProject(gradleModule, McpModelFG3::class.java) ?: return null

        var version: String? = null
        for (minecraftDepVersion in data.minecraftDepVersions) {
            val index = minecraftDepVersion.indexOf('-')
            if (index == -1) {
                continue
            }
            version = minecraftDepVersion.substring(0, minecraftDepVersion.indexOf('-'))
            break
        }

        val state = McpModuleSettings.State(
            version,
            data.mcpVersion,
            data.taskOutputLocation.absolutePath,
            SrgType.TSRG
        )

        ApplicationManager.getApplication().executeOnPooledThread {
            // Wait until indexing is done, to try and not interfere with current gradle process
            for (openProject in ProjectManager.getInstance().openProjects) {
                DumbService.getInstance(openProject).waitForSmartMode()
            }
            val project = ProjectManager.getInstance().openProjects.firstOrNull { it.name == gradleModule.project.name }

            var gradleProject = gradleModule.gradleProject
            val task = StringBuilder(data.taskName)
            if (gradleProject.parent != null) {
                task.insert(0, ':')
            }
            while (gradleProject.parent != null) {
                gradleProject = gradleProject.parent
                task.insert(0, gradleProject.name)
                task.insert(0, ':')
            }

            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating SRG map", false) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true

                    runGradleTask(project, gradleProject.projectDirectory, indicator) { launcher ->
                        launcher.forTasks(data.taskName)
                    }
                }
            })
        }

        return McpModelData(module, state)
    }
}
