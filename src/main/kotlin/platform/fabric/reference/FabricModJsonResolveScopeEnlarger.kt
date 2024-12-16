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

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.mcp.aw.AwFileType
import com.demonwav.mcdev.platform.mcp.fabricloom.FabricLoomData
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.search.SearchScope
import org.jetbrains.plugins.gradle.util.GradleUtil

class FabricModJsonResolveScopeEnlarger : ResolveScopeEnlarger() {

    override fun getAdditionalResolveScope(file: VirtualFile, project: Project): SearchScope? {
        if (file.name == FabricConstants.FABRIC_MOD_JSON) {
            val module = ModuleUtilCore.findModuleForFile(file, project)
                ?: return null
            return module.moduleWithDependentsScope.union(module.moduleTestsWithDependentsScope)
        }

        if (file.fileType is AwFileType) {
            var module = ModuleUtilCore.findModuleForFile(file, project)
                ?: return null

            val loomData = GradleUtil.findGradleModuleData(module)?.children
                ?.find { it.key == FabricLoomData.KEY }?.data as? FabricLoomData
                ?: return null

            var moduleManager = ModuleManager.getInstance(project)
            var baseModuleName = module.name.substringBeforeLast('.')
            var scope = module.moduleWithLibrariesScope
            for ((_, sourceSets) in loomData.modSourceSets.orEmpty()) {
                for (name in sourceSets) {
                    val otherModule = moduleManager.findModuleByName("$baseModuleName.$name") ?: continue
                    scope = scope.union(otherModule.moduleWithLibrariesScope)
                }
            }

            return scope
        }

        return null
    }
}
