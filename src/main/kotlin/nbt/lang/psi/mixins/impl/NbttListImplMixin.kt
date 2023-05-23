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

import com.demonwav.mcdev.nbt.MalformedNbtFileException
import com.demonwav.mcdev.nbt.lang.psi.getNbtTag
import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttListMixin
import com.demonwav.mcdev.nbt.tags.NbtTypeId
import com.demonwav.mcdev.nbt.tags.TagList
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttListImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttListMixin {

    override fun getListTag(): TagList {
        val tagList = getTagList()

        var type: NbtTypeId? = null
        for (nbttTag in tagList) {
            val tagType = nbttTag.getType()
            if (type == null) {
                type = tagType
            } else if (type != tagType) {
                throw MalformedNbtFileException("Lists can only contain elements of the same type.")
            }
        }

        if (tagList.isEmpty()) {
            type = NbtTypeId.END
        }

        assert(type != null)

        val tags = tagList.map {
            it.getNbtTag()
        }

        return TagList(type!!, tags)
    }
}
