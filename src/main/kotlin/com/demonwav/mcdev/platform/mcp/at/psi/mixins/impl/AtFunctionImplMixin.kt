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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtFunctionMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtFunctionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtFunctionMixin {

    override fun setArgumentList(argumentList: String) {
        val funcName = funcName.nameElement.text
        this.argumentList.forEach { it.delete() }

        val returnValue = returnValue.classValue!!.text
        replace(AtElementFactory.createFunction(project, "$funcName($argumentList)$returnValue"))
    }

    override fun setReturnValue(returnValue: String) {
        this.returnValue.replace(AtElementFactory.createReturnValue(project, returnValue))
    }

    override fun setFunction(function: String) {
        replace(AtElementFactory.createFunction(project, function))
    }
}
