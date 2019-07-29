/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.srg

import com.intellij.openapi.project.Project
import com.intellij.unscramble.UnscrambleSupport
import javax.swing.JComponent

class McpUnscrambler : UnscrambleSupport<JComponent> {

    private val srgPattern = Regex("(?:field|func)_\\d+_[a-zA-Z]+_?")

    override fun getPresentableName() = "Remap SRG names"

    override fun unscramble(project: Project, text: String, logName: String, settings: JComponent?): String? {
        val srgMap = SrgManager.findAnyInstance(project)?.srgMapNow ?: return null
        return srgPattern.replace(text) { srgMap.mapMcpToSrgName(it.value) ?: it.value }
    }
}
