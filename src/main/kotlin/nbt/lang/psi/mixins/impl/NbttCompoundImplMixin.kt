/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.nbt.lang.psi.mixins.impl

import com.demonwav.mcdev.nbt.lang.psi.getNbtTag
import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttCompoundMixin
import com.demonwav.mcdev.nbt.tags.NbtTag
import com.demonwav.mcdev.nbt.tags.TagCompound
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttCompoundImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttCompoundMixin {

    override fun getCompoundTag(): TagCompound {
        val map = mutableMapOf<String, NbtTag>()
        for (nbttNamedTag in getNamedTagList()) {
            map[nbttNamedTag.tagName.getTagName()] = nbttNamedTag.tag?.getNbtTag() ?: continue
        }
        return TagCompound(map)
    }
}
