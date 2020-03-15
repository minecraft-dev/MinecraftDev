/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.navigation

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.srg.McpSrgMap
import com.demonwav.mcdev.util.MemberReference
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class SrgMemberChooseByNameContributor : ChooseByNameContributor {

    // Cached between uses
    var srgMap: McpSrgMap? = null
    var module: Module? = null

    override fun getNames(project: Project, includeNonProjectItems: Boolean): Array<String> {
        if (!includeNonProjectItems) {
            return emptyArray()
        }

        val names = mutableSetOf<String>()

        val modules = ModuleManager.getInstance(project).modules
        for (module in modules) {
            val mcpModule = MinecraftFacet.getInstance(module, McpModuleType) ?: continue
            srgMap = mcpModule.srgManager?.srgMapNow ?: continue
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
        includeNonProjectItems: Boolean
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
