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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttFloatMixin
import com.demonwav.mcdev.nbt.tags.TagFloat
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttFloatImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttFloatMixin {

    override fun getFloatTag(): TagFloat {
        // Can't just regex out the f, since "Infinity" contains an f
        if (text.contains("Infinity")) {
            return TagFloat(text.trim().let { it.substring(0, it.length - 1) }.toFloat())
        }
        return TagFloat(text.trim().replace("[fF]".toRegex(), "").toFloat())
    }
}
