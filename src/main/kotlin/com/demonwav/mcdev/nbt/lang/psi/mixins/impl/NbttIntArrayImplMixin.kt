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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttIntArrayMixin
import com.demonwav.mcdev.nbt.tags.TagIntArray
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttIntArrayImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttIntArrayMixin {

    override fun getIntArrayTag(): TagIntArray {
        return TagIntArray(getIntList().map { it.getIntTag().value }.toIntArray())
    }
}
