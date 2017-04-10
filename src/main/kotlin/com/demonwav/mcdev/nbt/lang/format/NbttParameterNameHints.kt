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
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement

class NbttParameterNameHints : InlayParameterHintsProvider {
    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val list = mutableListOf<InlayInfo>()
        if (element is NbttCompound) {
            val size = element.getNamedTagList().size

            list.add(InlayInfo("$size ${if (size == 1) "child" else "children"}", element.textRange.startOffset + 1))
        } else if (element is NbttList) {
            val size = element.getListParams()?.tagList?.size ?: 0

            list.add(InlayInfo("$size ${if (size == 1) "child" else "children"}", element.textRange.startOffset + 1))
        } else if (element is NbttByteArray) {
            val size = element.getByteParams()?.byteList?.size ?: 0

            list.add(InlayInfo("$size ${if (size == 1) "child" else "children"}", element.node.getChildren(null)[1].textRange.startOffset + 1))
        } else if (element is NbttIntArray) {
            val size = element.getIntParams()?.intList?.size ?: 0

            list.add(InlayInfo("$size ${if (size == 1) "child" else "children"}", element.node.getChildren(null)[1].textRange.startOffset + 1))
        }
        return list
    }

    override fun getHintInfo(element: PsiElement) = null
    override fun getDefaultBlackList() = mutableSetOf<String>()
}
