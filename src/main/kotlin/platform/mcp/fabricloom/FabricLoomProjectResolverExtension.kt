/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.platform.mcp.gradle.McpModelData
import com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom.FabricLoomModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class FabricLoomProjectResolverExtension : AbstractProjectResolverExtension() {

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> =
        setOf(FabricLoomModel::class.java)

    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val loomData = resolverCtx.getExtraProject(gradleModule, FabricLoomModel::class.java)
        if (loomData != null) {
            val decompilers = loomData.decompilers.mapValues { (_, decompilers) ->
                decompilers.mapTo(mutableSetOf()) { decompiler ->
                    FabricLoomData.Decompiler(decompiler.name, decompiler.taskName, decompiler.sourcesPath)
                }
            }

            val data = FabricLoomData(
                ideModule.data,
                loomData.tinyMappings,
                decompilers,
                loomData.splitMinecraftJar,
                loomData.modSourceSets
            )
            ideModule.createChild(FabricLoomData.KEY, data)

            val mcpData = McpModelData(
                ideModule.data,
                McpModuleSettings.State(
                    minecraftVersion = loomData.minecraftVersion,
                ),
                null,
                null
            )
            ideModule.createChild(McpModelData.KEY, mcpData)

            for (child in ideModule.children) {
                val childData = child.data
                if (childData is GradleSourceSetData) {
                    child.createChild(McpModelData.KEY, mcpData.copy(module = childData))
                }
            }
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
