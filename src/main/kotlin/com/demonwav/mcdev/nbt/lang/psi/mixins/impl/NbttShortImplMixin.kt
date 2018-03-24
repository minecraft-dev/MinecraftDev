/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.psi.mixins.impl

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttShortMixin
import com.demonwav.mcdev.nbt.tags.TagShort
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttShortImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttShortMixin {

    override fun getShortTag(): TagShort {
        return TagShort(text.trim().replace(sRegex, "").toShort())
    }

    companion object {
        private val sRegex = "[sS]".toRegex()
    }
}
