/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.quiltloom

import com.demonwav.mcdev.platform.mcp.gradle.tooling.quiltloom.QuiltLoomModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class QuiltLoomProjectResolverExtension : AbstractProjectResolverExtension() {

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> =
        setOf(QuiltLoomModel::class.java)

    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val loomData = resolverCtx.getExtraProject(gradleModule, QuiltLoomModel::class.java)
        if (loomData != null) {
            val decompilers = loomData.decompilers.mapValues { (_, decompilers) ->
                decompilers.mapTo(mutableSetOf()) { decompiler ->
                    QuiltLoomData.Decompiler(decompiler.name, decompiler.taskName, decompiler.sourcesPath)
                }
            }

            val data = QuiltLoomData(ideModule.data, loomData.tinyMappings, decompilers, loomData.splitMinecraftJar)
            ideModule.createChild(QuiltLoomData.KEY, data)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
