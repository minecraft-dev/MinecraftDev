/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.psi.getClassFromString
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtReturnValueMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiClass

abstract class AtReturnValueImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtReturnValueMixin {

    override val returnValueClass: PsiClass?
        get() = getClassFromString(returnValueText, project)

    override val returnValueText: String
        get() = primitive?.text ?: classValue!!.text

    override fun setReturnValue(returnValue: String) {
        replace(AtElementFactory.createReturnValue(project, returnValue))
    }
}
