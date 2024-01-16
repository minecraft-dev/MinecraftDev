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

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.asset.MCDevBundle
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
                        children(size),
                        element.textRange.startOffset + 1,
                    ),
                )
            }
            is NbttList -> {
                // Size hint
                val size = element.getTagList().size
                list.add(
                    InlayInfo(
                        children(size),
                        element.textRange.startOffset + 1,
                    ),
                )

                if (size > 5) {
                    // Index hints
                    element.getTagList().forEachIndexed { i, param ->
                        list.add(InlayInfo("$i", param.textRange.startOffset))
                    }
                }
            }
            is NbttByteArray -> {
                val size = element.getByteList().size
                list.add(
                    InlayInfo(
                        children(size),
                        element.node.getChildren(null)[1].textRange.startOffset + 1,
                    ),
                )

                if (size > 5) {
                    // Index hints
                    element.getByteList().forEachIndexed { i, param ->
                        list.add(InlayInfo("$i", param.textRange.startOffset))
                    }
                }
            }
            is NbttIntArray -> {
                val size = element.getIntList().size
                list.add(
                    InlayInfo(
                        children(size),
                        element.node.getChildren(null)[1].textRange.startOffset + 1,
                    ),
                )

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
                        children(size),
                        element.node.getChildren(null)[1].textRange.startOffset + 1,
                    ),
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

    private fun children(size: Int): String {
        return if (size == 1) {
            MCDevBundle("nbt.lang.inlay_hints.one_child")
        } else {
            MCDevBundle("nbt.lang.inlay_hints.children", size)
        }
    }

    override fun getHintInfo(element: PsiElement): HintInfo? = null
    override fun getDefaultBlackList() = mutableSetOf<String>()
}
