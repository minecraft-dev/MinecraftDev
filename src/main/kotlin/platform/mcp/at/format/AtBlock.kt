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

package com.demonwav.mcdev.platform.mcp.at.format

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.children
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IFileElementType

class AtBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    val spacingBuilder: SpacingBuilder,
    val codeStyleSettings: CodeStyleSettings,
    val entryClassAlignment: Alignment? = null,
    val entryMemberAlignment: Alignment? = null,
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()

        var entryClassAlignment: Alignment? = entryClassAlignment
        var entryMemberAlignment: Alignment? = entryMemberAlignment

        var newlineCount = 0
        val alignGroups = node.elementType is IFileElementType &&
            codeStyleSettings.getCustomSettings(AtCodeStyleSettings::class.java).ALIGN_ENTRY_CLASS_AND_MEMBER
        for (child in node.children()) {
            val childType = child.elementType
            if (childType == TokenType.WHITE_SPACE) {
                continue
            }

            if (alignGroups) {
                if (childType == AtTypes.CRLF) {
                    newlineCount++
                    continue
                } else if (childType != AtTypes.COMMENT) {
                    if (newlineCount >= 2) {
                        // Align different groups separately, comments are not counted towards any group
                        entryClassAlignment = Alignment.createAlignment(true)
                        entryMemberAlignment = Alignment.createAlignment(true)
                    }
                    newlineCount = 0
                }
            }

            val alignment = when (childType) {
                AtTypes.CLASS_NAME -> entryClassAlignment
                AtTypes.FIELD_NAME, AtTypes.FUNCTION, AtTypes.ASTERISK -> entryMemberAlignment
                else -> null
            }

            blocks.add(
                AtBlock(
                    child,
                    null,
                    alignment,
                    spacingBuilder,
                    codeStyleSettings,
                    entryClassAlignment,
                    entryMemberAlignment
                )
            )
        }

        return blocks
    }

    override fun getIndent(): Indent? = Indent.getNoneIndent()

    override fun getChildIndent(): Indent? = Indent.getNoneIndent()

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = node.firstChildNode == null
}
