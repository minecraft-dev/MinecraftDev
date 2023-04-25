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
import com.demonwav.mcdev.util.findModule
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.plugins.gradle.util.GradleUtil

class FabricClientUseScopeEnlarger : UseScopeEnlarger() {

    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        val module = element.findModule()
            ?: return null
        val loomData = GradleUtil.findGradleModuleData(module)?.children
            ?.find { it.key == FabricLoomData.KEY }?.data as? FabricLoomData
            ?: return null

        if (loomData.splitMinecraftJar) {
            return GlobalSearchScope.filesScope(element.project) {
                val moduleWithDeps = GlobalSearchScope.moduleWithDependenciesScope(module)
                FilenameIndex.getVirtualFilesByName(FabricConstants.FABRIC_MOD_JSON, moduleWithDeps)
            }
        }

        return null
    }
}
