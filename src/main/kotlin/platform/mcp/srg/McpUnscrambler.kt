/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
