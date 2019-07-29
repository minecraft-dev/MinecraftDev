/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
