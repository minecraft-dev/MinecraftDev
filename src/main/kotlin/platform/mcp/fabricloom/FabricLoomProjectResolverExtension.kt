/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.platform.mcp.gradle.tooling.fabricloom.FabricLoomModel
import com.demonwav.mcdev.util.capitalize
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
            val gradleProjectPath = gradleModule.gradleProject.projectIdentifier.projectPath
            val suffix = if (gradleProjectPath.endsWith(':')) "" else ":"
            val decompileTasksNames = loomData.decompilers.mapTo(mutableSetOf()) { (rawName, sourcesPath) ->
                val name = rawName.capitalize()
                val taskName = gradleProjectPath + suffix + "genSourcesWith" + name
                FabricLoomData.Decompiler(name, taskName, sourcesPath)
            }
            val data = FabricLoomData(ideModule.data, loomData.tinyMappings, decompileTasksNames)
            println("Loaded $data")
            ideModule.createChild(FabricLoomData.KEY, data)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
