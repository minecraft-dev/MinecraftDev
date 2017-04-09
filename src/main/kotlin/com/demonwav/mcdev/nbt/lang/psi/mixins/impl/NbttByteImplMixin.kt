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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttByteMixin
import com.demonwav.mcdev.nbt.tags.TagByte
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttByteImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttByteMixin {

    override fun getByteTag(): TagByte {
        return TagByte(text.trim().replace("[bB]".toRegex(), "").toByte())
    }
}
