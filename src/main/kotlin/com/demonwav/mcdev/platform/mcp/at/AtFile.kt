/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.FileViewProvider

class AtFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AtLanguage) {

    init {
        setup()
    }

    private fun setup() {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        val vFile = viewProvider.virtualFile

        val module = ModuleUtilCore.findModuleForFile(vFile, project) ?: return
        val mcpModule = MinecraftFacet.getInstance(module, McpModuleType) ?: return
        mcpModule.addAccessTransformerFile(vFile)
    }

    override fun getFileType() = AtFileType
    override fun toString() = "Access Transformer File"
    override fun getIcon(flags: Int) = PlatformAssets.MCP_ICON
}
