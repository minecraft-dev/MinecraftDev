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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttByteMixin
import com.demonwav.mcdev.nbt.tags.TagByte
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.apache.commons.lang3.StringUtils

abstract class NbttByteImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttByteMixin {

    override fun getByteTag(): TagByte {
        return when (text) {
            "false" -> TagByte(0)
            "true" -> TagByte(1)
            else -> TagByte(StringUtils.replaceChars(text.trim(), "bB", null).toByte())
        }
    }
}
