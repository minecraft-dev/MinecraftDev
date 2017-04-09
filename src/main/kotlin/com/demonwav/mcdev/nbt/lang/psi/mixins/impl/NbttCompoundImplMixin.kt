/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi.mixins.impl

import com.demonwav.mcdev.nbt.lang.psi.getNtbTag
import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttCompoundMixin
import com.demonwav.mcdev.nbt.tags.NbtTag
import com.demonwav.mcdev.nbt.tags.TagCompound
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttCompoundImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttCompoundMixin {

    override fun getCompoundTag(): TagCompound {
        val map = mutableMapOf<String, NbtTag>()
        for (nbttNamedTag in getNamedTagList()) {
            map[nbttNamedTag.tagName.getTagName()] = nbttNamedTag.tag.getNtbTag()
        }
        return TagCompound(map)
    }
}
