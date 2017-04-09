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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttLongMixin
import com.demonwav.mcdev.nbt.tags.TagLong
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttLongImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttLongMixin {

    override fun getLongTag(): TagLong {
        return TagLong(text.trim().replace("[lL]".toRegex(), "").toLong())
    }
}
