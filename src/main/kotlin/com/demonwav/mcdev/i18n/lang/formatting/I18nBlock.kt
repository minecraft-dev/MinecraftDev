/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.formatting

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.formatting.WrapType
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import java.util.ArrayList

class I18nBlock(node: ASTNode, wrap: Wrap?, alignment: Alignment?, private val spacingBuilder: SpacingBuilder) :
    AbstractBlock(node, wrap, alignment) {
    override fun buildChildren(): List<Block> {
        val blocks = ArrayList<Block>()
        var child: ASTNode? = myNode.firstChildNode
        var previousChild: ASTNode? = null
        while (child != null) {
            if (
                child.elementType !== TokenType.WHITE_SPACE && (previousChild == null ||
                    previousChild.elementType !== I18nTypes.LINE_ENDING || child.elementType !== I18nTypes.LINE_ENDING)
            ) {
                val block =
                    I18nBlock(child, Wrap.createWrap(WrapType.NONE, false), Alignment.createAlignment(), spacingBuilder)
                blocks.add(block)
            }
            previousChild = child
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent() = Indent.getNoneIndent()

    override fun getSpacing(child1: Block?, child2: Block) = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf() = myNode.firstChildNode == null
}
