/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.mcp.fabricloom.FabricLoomData
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.plugins.gradle.util.GradleUtil

class FabricModJsonResolveScopeEnlarger : ResolveScopeEnlarger() {

    override fun getAdditionalResolveScope(file: VirtualFile, project: Project): SearchScope? {
        if (file.name != FabricConstants.FABRIC_MOD_JSON) {
            return null
        }

        val module = ModuleUtilCore.findModuleForFile(file, project)
            ?: return null
        val loomData = GradleUtil.findGradleModuleData(module)?.children
            ?.find { it.key == FabricLoomData.KEY }?.data as? FabricLoomData
            ?: return null

        val moduleScopes = mutableListOf<GlobalSearchScope>()
        val moduleManager = ModuleManager.getInstance(project)
        val parentPath = module.name.substringBeforeLast('.')
        for ((_, sourceSets) in loomData.modSourceSets) {
            for (sourceSet in sourceSets) {
                val childModule = moduleManager.findModuleByName("$parentPath.$sourceSet")
                if (childModule != null) {
                    moduleScopes.add(childModule.getModuleScope(false))
                }
            }
        }

        if (moduleScopes.isEmpty()) {
            return null
        }

        return GlobalSearchScope.union(moduleScopes)
    }
}
