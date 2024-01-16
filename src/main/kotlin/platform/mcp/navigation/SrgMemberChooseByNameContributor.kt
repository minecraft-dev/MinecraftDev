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

package com.demonwav.mcdev.platform.mcp.navigation

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.mappings.Mappings
import com.demonwav.mcdev.util.MemberReference
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class SrgMemberChooseByNameContributor : ChooseByNameContributor {

    // Cached between uses
    var srgMap: Mappings? = null
    var module: Module? = null

    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        if (!includeNonProjectItems) {
            return emptyArray()
        }

        val names = mutableSetOf<String>()

        val modules = ModuleManager.getInstance(project).modules
        for (module in modules) {
            val mcpModule = MinecraftFacet.getInstance(module, McpModuleType) ?: continue
            srgMap = mcpModule.mappingsManager?.mappingsNow ?: continue
            this.module = module
            break // for speed's sake, only use the first one found
        }

        srgMap?.apply {
            fieldMap.values.mapTo(names) { it.name }
            methodMap.values.mapTo(names) { it.name + it.descriptor }
        }

        return names.toTypedArray()
    }

    override fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean,
    ): Array<NavigationItem> {
        if (!includeNonProjectItems || srgMap == null || module == null) {
            return emptyArray()
        }

        var memberRef: MemberReference? = null
        if (name.startsWith("field")) {
            for ((mcp, srg) in srgMap!!.fieldMap) {
                if (name == srg.name) {
                    memberRef = mcp
                    break
                }
            }
        } else if (name.startsWith("func")) {
            for ((mcp, srg) in srgMap!!.methodMap) {
                if (name == srg.name + srg.descriptor) {
                    memberRef = mcp
                    break
                }
            }
        }

        try {
            memberRef?.let {
                val member =
                    it.resolveMember(project, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module!!))
                        ?: return emptyArray()
                return arrayOf(member)
            }
            return emptyArray()
        } finally {
            srgMap = null // reset for next run
            module = null
        }
    }
}
