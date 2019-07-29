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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttByteArrayMixin
import com.demonwav.mcdev.nbt.tags.TagByteArray
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttByteArrayImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttByteArrayMixin {

    override fun getByteArrayTag(): TagByteArray {
        return TagByteArray(getByteList().map { it.getByteTag().value }.toByteArray())
    }
}
