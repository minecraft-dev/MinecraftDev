/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttCompound
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttIntArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttList
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

class NbttFoldingBuilder : FoldingBuilder {
    override fun getPlaceholderText(node: ASTNode): String? {
        return when (node.elementType) {
            NbttTypes.BYTE_ARRAY, NbttTypes.INT_ARRAY, NbttTypes.LIST -> "..."
            NbttTypes.COMPOUND -> {
                val tagList = (node.psi as NbttCompound).namedTagList
                val tag = tagList[0].tag
                if (tagList.size == 1 && tag.list == null && tag.compound == null && tag.intArray == null && tag.byteArray == null) {
                    tagList[0].text
                } else {
                    "..."
                }
            }
            else -> null
        }
    }

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val list = mutableListOf<FoldingDescriptor>()
        foldChildren(node, list)
        return list.toTypedArray()
    }

    private fun foldChildren(node: ASTNode, list: MutableList<FoldingDescriptor>) {
        if (node.elementType == NbttTypes.COMPOUND) {
            val lbrace = node.findChildByType(NbttTypes.LBRACE)
            val rbrace = node.findChildByType(NbttTypes.RBRACE)
            if (lbrace != null && rbrace != null) {
                list.add(FoldingDescriptor(node, TextRange(lbrace.textRange.endOffset, rbrace.textRange.startOffset)))
            }
        } else if (node.elementType == NbttTypes.LIST) {
            val lbracket = node.findChildByType(NbttTypes.LBRACKET)
            val rbracket = node.findChildByType(NbttTypes.RBRACKET)
            if (lbracket != null && rbracket != null) {
                list.add(FoldingDescriptor(node, TextRange(lbracket.textRange.endOffset, rbracket.textRange.startOffset)))
            }
        } else if (node.elementType == NbttTypes.INT_ARRAY || node.elementType == NbttTypes.BYTE_ARRAY) {
            val lparen = node.findChildByType(NbttTypes.LPAREN)
            val rparen = node.findChildByType(NbttTypes.RPAREN)
            if (lparen != null && rparen != null) {
                list.add(FoldingDescriptor(node, TextRange(lparen.textRange.endOffset, rparen.textRange.startOffset)))
            }
        }

        node.getChildren(null).forEach { foldChildren(it, list) }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        val psi = node.psi

        val size = if (psi is NbttByteArray) {
            psi.byteParams?.byteList?.size ?: 0
        } else if (psi is NbttIntArray) {
            psi.intParams?.intList?.size ?: 0
        } else if (psi is NbttList) {
            psi.listParams?.tagList?.size ?: 0
        } else if (psi is NbttCompound) {
            if (psi.namedTagList.size == 1) {
                val tag = psi.namedTagList[0].tag
                if (tag.list == null && tag.compound == null && tag.intArray == null && tag.byteArray == null) {
                    return true
                }
            }
            psi.namedTagList.size
        } else {
            0
        }

        return size > 50 // TODO arbitrary? make a setting?
    }
}
