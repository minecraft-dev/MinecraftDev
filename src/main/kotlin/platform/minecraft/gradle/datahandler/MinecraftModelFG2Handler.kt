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
import com.demonwav.mcdev.platform.minecraft.gradle.MinecraftModelData
import com.demonwav.mcdev.platform.minecraft.gradle.tooling.MinecraftModelFG2
import com.demonwav.mcdev.platform.minecraft.srg.SrgType
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext

object MinecraftModelFG2Handler : MinecraftModelDataHandler {

    override fun build(
        gradleModule: IdeaModule,
        node: DataNode<ModuleData>,
        resolverCtx: ProjectResolverContext,
    ) {
        val data = resolverCtx.getExtraProject(gradleModule, MinecraftModelFG2::class.java) ?: return

        val state = MinecraftModuleSettings.State(
            data.minecraftVersion,
            data.mcpVersion,
            data.mappingFiles.find { it.endsWith("mcp-srg.srg") },
            SrgType.SRG,
        )

        val modelData = MinecraftModelData(
            node.data,
            state,
            null,
            null,
        )

        node.createChild(
            MinecraftModelData.KEY,
            MinecraftModelData(
                node.data,
                MinecraftModuleSettings.State(
                    data.minecraftVersion,
                    data.mcpVersion,
                    data.mappingFiles.find { it.endsWith("mcp-srg.srg") },
                    SrgType.SRG,
                ),
                null,
                null,
            ),
        )

        for (child in node.children) {
            val childData = child.data
            if (childData is GradleSourceSetData) {
                child.createChild(MinecraftModelData.KEY, modelData.copy(module = childData))
            }
        }
    }
}
