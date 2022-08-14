/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom.FabricLoomModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
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

            val data = FabricLoomData(ideModule.data, loomData.tinyMappings, decompilers, loomData.splitMinecraftJar)
            ideModule.createChild(FabricLoomData.KEY, data)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
