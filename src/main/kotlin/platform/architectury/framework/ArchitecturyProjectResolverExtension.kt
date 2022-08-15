/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.architectury.framework

import com.demonwav.mcdev.platform.mcp.gradle.tooling.archloom.ArchitecturyModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class ArchitecturyProjectResolverExtension : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> =
        setOf(ArchitecturyModel::class.java)

    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val archData = resolverCtx.getExtraProject(gradleModule, ArchitecturyModel::class.java)
        if (archData != null) {
            val gradleData = ArchitecturyGradleData(ideModule.data, archData.moduleType)
            ideModule.createChild(ArchitecturyGradleData.KEY, gradleData)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
