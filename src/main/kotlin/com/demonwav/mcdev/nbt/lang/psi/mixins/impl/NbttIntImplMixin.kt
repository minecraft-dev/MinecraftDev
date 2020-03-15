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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttIntMixin
import com.demonwav.mcdev.nbt.tags.TagInt
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.apache.commons.lang3.StringUtils

abstract class NbttIntImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttIntMixin {

    override fun getIntTag(): TagInt {
        return TagInt(StringUtils.replaceChars(text.trim(), "iI", null).toInt())
    }
}
