/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.minecraft.gradle.datahandler

import com.demonwav.mcdev.platform.minecraft.MinecraftModuleSettings
import com.demonwav.mcdev.platform.minecraft.at.AtFileType
import com.demonwav.mcdev.platform.minecraft.gradle.MinecraftModelData
import com.demonwav.mcdev.platform.minecraft.gradle.tooling.MinecraftModelFG3
import com.demonwav.mcdev.platform.minecraft.srg.SrgType
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.LocalFileSystem
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.gradleIdentityPath

object MinecraftModelFG3Handler : MinecraftModelDataHandler {

    override fun build(
        gradleModule: IdeaModule,
        node: DataNode<ModuleData>,
        resolverCtx: ProjectResolverContext,
    ) {
        val data = resolverCtx.getExtraProject(gradleModule, MinecraftModelFG3::class.java) ?: return

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

        val state = MinecraftModuleSettings.State(
            mcVersion,
            data.mcpVersion,
            data.taskOutputLocation.absolutePath,
            SrgType.TSRG,
            forgeVersion,
        )

        // gradleIdentityPath makes it work with composite builds
        val identityPath = node.data.gradleIdentityPath
        // But ignore it if it is the root project, as taskName already starts with a colon
        val taskName = if (identityPath == ":") data.taskName else identityPath + ':' + data.taskName

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

        val modelData = MinecraftModelData(node.data, state, taskName, data.accessTransformers)
        node.createChild(MinecraftModelData.KEY, modelData)

        for (child in node.children) {
            val childData = child.data
            if (childData is GradleSourceSetData) {
                child.createChild(MinecraftModelData.KEY, modelData.copy(module = childData))
            }
        }
    }
}
