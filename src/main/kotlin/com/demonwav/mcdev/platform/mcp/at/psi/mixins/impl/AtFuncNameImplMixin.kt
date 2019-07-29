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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtFuncNameMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtFuncNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtFuncNameMixin {

    override fun setFuncName(funcName: String) {
        replace(AtElementFactory.createFuncName(project, funcName))
    }

    override val funcNameText: String
        get() = nameElement.text
}
