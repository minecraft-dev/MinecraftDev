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

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttByteArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttCompound
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttIntArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttList
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttLongArray
import com.demonwav.mcdev.nbt.lang.gen.psi.NbttNamedTag
import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement

class NbttParameterNameHints : InlayParameterHintsProvider {
    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val list = mutableListOf<InlayInfo>()
        when (element) {
            is NbttCompound -> {
                val size = element.getNamedTagList().size
                list.add(
                    InlayInfo(
                        "$size ${if (size == 1) "child" else "children"}",
                        element.textRange.startOffset + 1
                    )
                )
            }
            is NbttList -> {
                // Size hint
                val size = element.getTagList().size
                if (size > 50) {
                    list.add(
                        InlayInfo(
                            "$size ${if (size == 1) "child" else "children"}",
                            element.textRange.startOffset + 1
                        )
                    )
                }

                if (size > 5) {
                    // Index hints
                    element.getTagList().forEachIndexed { i, param ->
                        list.add(InlayInfo("$i", param.textRange.startOffset))
                    }
                }
            }
            is NbttByteArray -> {
                val size = element.getByteList().size
                if (size > 50) {
                    list.add(
                        InlayInfo(
                            "$size ${if (size == 1) "child" else "children"}",
                            element.node.getChildren(null)[1].textRange.startOffset + 1
                        )
                    )
                }

                if (size > 5) {
                    // Index hints
                    element.getByteList().forEachIndexed { i, param ->
                        list.add(InlayInfo("$i", param.textRange.startOffset))
                    }
                }
            }
            is NbttIntArray -> {
                val size = element.getIntList().size
                if (size > 50) {
                    list.add(
                        InlayInfo(
                            "$size ${if (size == 1) "child" else "children"}",
                            element.node.getChildren(null)[1].textRange.startOffset + 1
                        )
                    )
                }

                if (size > 5) {
                    // Index hints
                    element.getIntList().forEachIndexed { i, param ->
                        list.add(InlayInfo("$i", param.textRange.startOffset))
                    }
                }
            }
            is NbttLongArray -> {
                val size = element.getLongList().size
                list.add(
                    InlayInfo(
                        "$size ${if (size == 1) "child" else "children"}",
                        element.node.getChildren(null)[1].textRange.startOffset + 1
                    )
                )

                // Index hints
                element.getLongList().forEachIndexed { i, param ->
                    list.add(InlayInfo("$i", param.textRange.startOffset))
                }
            }
            is NbttNamedTag -> {
                val tag = element.tag
                val text = when {
                    tag?.getByte() != null -> "byte"
                    tag?.getShort() != null -> "short"
                    tag?.getInt() != null -> "int"
                    tag?.getLong() != null -> "long"
                    tag?.getFloat() != null -> "float"
                    tag?.getDouble() != null -> "double"
                    else -> null
                }

                if (text != null) {
                    list.add(InlayInfo(text, element.node.startOffset))
                }
            }
        }

        return list
    }

    override fun getHintInfo(element: PsiElement): HintInfo? = null
    override fun getDefaultBlackList() = mutableSetOf<String>()
}
