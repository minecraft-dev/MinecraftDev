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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttDoubleMixin
import com.demonwav.mcdev.nbt.tags.TagDouble
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttDoubleImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttDoubleMixin {

    override fun getDoubleTag(): TagDouble {
        return TagDouble(text.trim().replace("[dD|]".toRegex(), "").toDouble())
    }
}
