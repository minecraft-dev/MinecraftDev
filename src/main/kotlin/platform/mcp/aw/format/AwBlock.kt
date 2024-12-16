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

package com.demonwav.mcdev.platform.mcp.aw.format

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
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

class AwBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    val spacingBuilder: SpacingBuilder,
    val codeStyleSettings: CodeStyleSettings,
    val targetKindAlignment: Alignment? = null,
    val entryClassAlignment: Alignment? = null,
    val entryMemberAlignment: Alignment? = null,
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()

        var targetKindAlignment: Alignment? = targetKindAlignment
        var entryClassAlignment: Alignment? = entryClassAlignment
        var entryMemberAlignment: Alignment? = entryMemberAlignment

        var newlineCount = 0
        val alignGroups = node.elementType is IFileElementType &&
            codeStyleSettings.getCustomSettings(AwCodeStyleSettings::class.java).ALIGN_ENTRY_CLASS_AND_MEMBER
        for (child in node.children()) {
            val childType = child.elementType
            if (childType == TokenType.WHITE_SPACE) {
                continue
            }

            if (alignGroups) {
                if (childType == AwTypes.CRLF) {
                    newlineCount++
                    continue
                } else if (childType != AwTypes.COMMENT) {
                    if (newlineCount >= 2) {
                        // Align different groups separately, comments are not counted towards any group
                        targetKindAlignment = Alignment.createAlignment(true)
                        entryClassAlignment = Alignment.createAlignment(true)
                        entryMemberAlignment = Alignment.createAlignment(true)
                    }
                    newlineCount = 0
                }
            }

            val alignment = when (childType) {
                AwTypes.CLASS_ELEMENT, AwTypes.FIELD_ELEMENT, AwTypes.METHOD_ELEMENT -> targetKindAlignment
                AwTypes.CLASS_NAME -> entryClassAlignment
                AwTypes.MEMBER_NAME, AwTypes.FIELD_DESC, AwTypes.METHOD_DESC -> entryMemberAlignment
                else -> null
            }

            blocks.add(
                AwBlock(
                    child,
                    null,
                    alignment,
                    spacingBuilder,
                    codeStyleSettings,
                    targetKindAlignment,
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
