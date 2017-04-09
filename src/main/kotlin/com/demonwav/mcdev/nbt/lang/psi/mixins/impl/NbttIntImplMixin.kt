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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttIntMixin
import com.demonwav.mcdev.nbt.tags.TagInt
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttIntImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttIntMixin {

    override fun getIntTag(): TagInt {
        return TagInt(text.trim().replace("[iI]".toRegex(), "").toInt())
    }
}
