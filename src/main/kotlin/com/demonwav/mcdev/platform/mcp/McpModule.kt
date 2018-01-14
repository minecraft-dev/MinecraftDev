/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

class McpModule(facet: MinecraftFacet) : AbstractModule(facet) {

    private val settings: McpModuleSettings = McpModuleSettings.getInstance(module)
    val accessTransformers = mutableSetOf<VirtualFile>()

    var srgManager: SrgManager? = null
        private set

    init {
        val files = getSettings().mappingFiles
        if (!files.isEmpty()) {
            srgManager = SrgManager.getInstance(files)
            srgManager!!.parse()
        }
    }

    override val moduleType  = McpModuleType
    override val type = PlatformType.MCP
    override val icon = null

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    fun getSettings() = settings.state

    fun updateSettings(data: McpModuleSettings.State) {
        this.settings.loadState(data)
        srgManager = SrgManager.getInstance(data.mappingFiles)
        srgManager!!.parse()
    }

    fun addAccessTransformerFile(file: VirtualFile) {
        accessTransformers.add(file)
    }

    override fun dispose() {
        super.dispose()

        accessTransformers.clear()
        srgManager = null
    }
}
