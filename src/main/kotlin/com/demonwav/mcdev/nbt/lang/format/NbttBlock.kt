/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.nbt.lang.NbttParserDefinition
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttCompound
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttIntArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttList
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLongArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Wrap
import com.intellij.formatting.WrapType
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings

class NbttBlock(
    private val node: ASTNode,
    private val settings: CodeStyleSettings,
    private val indent: Indent,
    private val wrap: Wrap?
) : ASTBlock {

    private val psiElement = node.psi
    private val childWrap: Wrap?
    private val spacingBuilder = NbttFormattingModelBuilder.createSpacingBuilder(settings)

    init {
        childWrap = when (psiElement) {
            is NbttCompound -> {
                Wrap.createWrap(CommonCodeStyleSettings.WRAP_ALWAYS, true)
            }
            is NbttList -> {
                Wrap.createWrap(getCustomSettings().LIST_WRAPPING, false)
            }
            is NbttByteArray, is NbttIntArray, is NbttLongArray -> {
                Wrap.createWrap(getCustomSettings().ARRAY_WRAPPING, false)
            }
            else -> null
        }
    }

    override fun isIncomplete(): Boolean {
        val lastChildNode = node.lastChildNode
        if (NbttTypes.COMPOUND == node.elementType) {
            return lastChildNode != null && lastChildNode != NbttTypes.RBRACE
        }
        if (
            NbttTypes.INT_ARRAY == node.elementType ||
            NbttTypes.BYTE_ARRAY == node.elementType ||
            NbttTypes.LONG_ARRAY == node.elementType
        ) {
            return lastChildNode != null && lastChildNode.elementType != NbttTypes.RPAREN
        }
        if (NbttTypes.LIST == node.elementType) {
            return lastChildNode != null && lastChildNode.elementType != NbttTypes.RBRACKET
        }
        return false
    }

    override fun isLeaf() = node.firstChildNode == null
    override fun getSpacing(child1: Block?, child2: Block) = spacingBuilder.getSpacing(this, child1, child2)
    override fun getTextRange() = node.textRange!!

    override fun getSubBlocks(): MutableList<Block> = subBlocksDelegate
    private val subBlocksDelegate: MutableList<Block> by lazy {
        node.getChildren(null).mapNotNull { node ->
            if (node.isWhitespaceOrEmpty) {
                null
            } else {
                makeSubBlock(node)
            }
        }.toMutableList()
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        if (NbttParserDefinition.NBTT_CONTAINERS.contains(node.elementType)) {
            return ChildAttributes(Indent.getNormalIndent(), null)
        }
        if (psiElement is PsiFile) {
            return ChildAttributes(Indent.getNoneIndent(), null)
        }
        return ChildAttributes(null, null)
    }

    override fun getAlignment(): Alignment? = null
    override fun getWrap() = wrap
    override fun getIndent() = indent
    override fun getNode() = node

    private fun makeSubBlock(childNode: ASTNode): Block {
        var indent = Indent.getNoneIndent()
        var wrap: Wrap? = null

        if (NbttParserDefinition.NBTT_CONTAINERS.contains(node.elementType)) {
            if (NbttTypes.COMMA == childNode.elementType) {
                wrap = Wrap.createWrap(WrapType.NONE, true)
            } else if (!NbttParserDefinition.NBTT_BRACES.contains(childNode.elementType)) {
                wrap = childWrap!!
                indent = Indent.getNormalIndent()
            }
        }
        return NbttBlock(childNode, settings, indent, wrap)
    }

    private fun getCustomSettings() = settings.getCustomSettings(NbttCodeStyleSettings::class.java)
}

private val ASTNode.isWhitespaceOrEmpty
    get() = this.elementType == TokenType.WHITE_SPACE || this.textLength == 0
