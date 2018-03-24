/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle

import com.demonwav.mcdev.platform.forge.gradle.tooling.ForgePatcherModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class ForgePatcherProjectResolverExtension : AbstractProjectResolverExtension() {

    override fun getExtraProjectModelClasses() = setOf(ForgePatcherModel::class.java)
    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val model = resolverCtx.getExtraProject(gradleModule, ForgePatcherModel::class.java)
        if (model != null) {
            ideModule.createChild(ForgePatcherModelData.KEY, ForgePatcherModelData(ideModule.data, model))
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
