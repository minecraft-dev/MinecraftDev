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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttDoubleMixin
import com.demonwav.mcdev.nbt.tags.TagDouble
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.apache.commons.lang3.StringUtils

abstract class NbttDoubleImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttDoubleMixin {

    override fun getDoubleTag(): TagDouble {
        return TagDouble(StringUtils.replaceChars(text.trim(), "dD", null).toDouble())
    }
}
