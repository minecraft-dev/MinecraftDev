/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.unscramble.UnscrambleSupport
import javax.swing.JComponent

class McpUnscrambler : UnscrambleSupport<JComponent> {

    private val srgPattern = Regex("(?:field|func)_\\d+_[a-zA-Z]+_?")

    override fun getPresentableName() = "Remap SRG names"

    override fun unscramble(project: Project, text: String, logName: String, settings: JComponent?): String? {
        val srgMap = ModuleManager.getInstance(project).modules.mapFirstNotNull {
            MinecraftFacet.getInstance(it, McpModuleType)?.srgManager?.srgMapNow
        } ?: return null
        return srgPattern.replace(text) { srgMap.mapSrgName(it.value) ?: it.value }
    }
}
