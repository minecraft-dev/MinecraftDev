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
import com.demonwav.mcdev.platform.mcp.at.AtFileType
import com.demonwav.mcdev.platform.mcp.gradle.McpModelData
import com.demonwav.mcdev.platform.mcp.gradle.tooling.McpModelFG3
import com.demonwav.mcdev.platform.mcp.srg.SrgType
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.LocalFileSystem
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

object McpModelFG3Handler : McpModelDataHandler {

    override fun build(
        gradleModule: IdeaModule,
        node: DataNode<ModuleData>,
        resolverCtx: ProjectResolverContext
    ) {
        val data = resolverCtx.getExtraProject(gradleModule, McpModelFG3::class.java) ?: return

        var mcVersion: String? = null
        var forgeVersion: String? = null
        for (minecraftDepVersion in data.minecraftDepVersions) {
            val index = minecraftDepVersion.indexOf('-')
            if (index == -1) {
                continue
            }
            mcVersion = minecraftDepVersion.substring(0, index)

            val forgeVersionEnd = minecraftDepVersion.indexOf('_')
            if (forgeVersionEnd != -1 && forgeVersionEnd > index) {
                forgeVersion = minecraftDepVersion.substring(index + 1, forgeVersionEnd)
            }
            break
        }

        val state = McpModuleSettings.State(
            mcVersion,
            data.mcpVersion,
            data.taskOutputLocation.absolutePath,
            SrgType.TSRG,
            forgeVersion
        )

        val gradleProjectPath = gradleModule.gradleProject.projectIdentifier.projectPath
        val suffix = if (gradleProjectPath.endsWith(':')) "" else ":"
        val taskName = gradleProjectPath + suffix + data.taskName

        val ats = data.accessTransformers
        if (ats != null && ats.isNotEmpty()) {
            runWriteTaskLater {
                for (at in ats) {
                    val fileTypeManager = FileTypeManager.getInstance()
                    val atFile = LocalFileSystem.getInstance().findFileByIoFile(at) ?: continue
                    fileTypeManager.associate(AtFileType, ExactFileNameMatcher(atFile.name))
                }
            }
        }

        val modelData = McpModelData(node.data, state, taskName, data.accessTransformers)
        node.createChild(McpModelData.KEY, modelData)

        for (child in node.children) {
            val childData = child.data
            if (childData is GradleSourceSetData) {
                child.createChild(McpModelData.KEY, modelData.copy(module = childData))
            }
        }
    }
}
