/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttIntArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class NbttBraceMatcher : PairedBraceMatcher {
    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int {
        val element = file.findElementAt(openingBraceOffset)
        if (element == null || element is PsiFile) {
            return openingBraceOffset
        }

        val parent = element.parent

        if (parent is NbttIntArray || parent is NbttByteArray) {
            return parent.textRange.startOffset
        }

        return openingBraceOffset
    }

    override fun getPairs() = arrayOf(
        BracePair(NbttTypes.LPAREN, NbttTypes.RPAREN, true),
        BracePair(NbttTypes.LBRACE, NbttTypes.RBRACE, true),
        BracePair(NbttTypes.LBRACKET, NbttTypes.RBRACKET, true)
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true
}
