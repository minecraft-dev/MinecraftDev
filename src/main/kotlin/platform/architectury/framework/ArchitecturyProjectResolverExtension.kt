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
