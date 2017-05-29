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

import com.demonwav.mcdev.nbt.lang.psi.mixins.NbttRootCompoundMixin
import com.demonwav.mcdev.nbt.tags.RootCompound
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class NbttRootCompoundImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), NbttRootCompoundMixin {

    override fun getRootCompoundTag(): RootCompound {
        return RootCompound(getTagName().getTagName(), getCompound().getCompoundTag().tagMap)
    }
}
