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

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttIntArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLongArray
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

        if (parent is NbttByteArray || parent is NbttIntArray || parent is NbttLongArray) {
            return parent.textRange.startOffset
        }

        return openingBraceOffset
    }

    override fun getPairs() = arrayOf(
        BracePair(NbttTypes.LPAREN, NbttTypes.RPAREN, true),
        BracePair(NbttTypes.LBRACE, NbttTypes.RBRACE, true),
        BracePair(NbttTypes.LBRACKET, NbttTypes.RBRACKET, true),
    )

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?) = true
}
