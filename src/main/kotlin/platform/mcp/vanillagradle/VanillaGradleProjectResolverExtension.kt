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

package com.demonwav.mcdev.platform.mcp.vanillagradle

import com.demonwav.mcdev.platform.mcp.gradle.tooling.vanillagradle.VanillaGradleModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

class VanillaGradleProjectResolverExtension : AbstractProjectResolverExtension() {

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> =
        setOf(VanillaGradleModel::class.java)

    override fun getToolingExtensionsClasses() = extraProjectModelClasses

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val vgData = resolverCtx.getExtraProject(gradleModule, VanillaGradleModel::class.java)
        if (vgData != null && vgData.hasVanillaGradle()) {
            val gradleProjectPath = gradleModule.gradleProject.projectIdentifier.projectPath
            val suffix = if (gradleProjectPath.endsWith(':')) "" else ":"
            val decompileTaskName = gradleProjectPath + suffix + "decompile"
            ideModule.createChild(VanillaGradleData.KEY, VanillaGradleData(ideModule.data, decompileTaskName))
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}
