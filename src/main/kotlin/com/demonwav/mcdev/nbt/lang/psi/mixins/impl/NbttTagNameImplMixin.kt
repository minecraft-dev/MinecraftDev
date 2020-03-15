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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttTagNameMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttTagNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttTagNameMixin {

    override fun getTagName() = getNbtStringValue(text)
}
