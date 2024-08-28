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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class AtFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AtLanguage) {

    init {
        setup()
    }

    val headComments: List<PsiComment>
        get() {
            val comments = mutableListOf<PsiComment>()
            for (child in children) {
                if (child is AtEntry) {
                    break
                }

                if (child is PsiComment) {
                    comments.add(child)
                }
            }

            return comments
        }

    fun addHeadComment(text: String) {
        val toAdd = text.lines().flatMap { listOf(AtElementFactory.createComment(project, it)) }
        val lastHeadComment = headComments.lastOrNull()
        if (lastHeadComment == null) {
            for (comment in toAdd.reversed()) {
                addAfter(comment, null)
            }
        } else {
            var previousComment: PsiElement? = lastHeadComment
            for (comment in toAdd) {
                previousComment = addAfter(comment, previousComment)
            }
        }
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
    override fun toString() = AtFileType.description
    override fun getIcon(flags: Int) = PlatformAssets.MCP_ICON
}
