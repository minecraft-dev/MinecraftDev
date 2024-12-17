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

package com.demonwav.mcdev.platform.mcp.aw

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwEntry
import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwHeader
import com.demonwav.mcdev.util.childrenOfType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

class AwFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AwLanguage) {

    val header: AwHeader?
        get() = children.firstOrNull { it is AwHeader } as? AwHeader

    val entries: Collection<AwEntry>
        get() = childrenOfType()

    val headComments: List<PsiComment>
        get() {
            val comments = mutableListOf<PsiComment>()
            for (child in children) {
                if (child is AwEntry) {
                    break
                }

                if (child is PsiComment) {
                    comments.add(child)
                }
            }

            return comments
        }

    fun addHeadComment(text: String) {
        val toAdd = text.lines().map { AwElementFactory.createComment(project, it) }
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

    override fun getFileType() = AwFileType
    override fun toString() = "Access Widener File"
    override fun getIcon(flags: Int) = PlatformAssets.MCP_ICON
}
