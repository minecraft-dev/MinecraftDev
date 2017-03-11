/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtArgument
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFuncName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtReturnValue
import com.intellij.psi.PsiElement

interface AtFunctionMixin : PsiElement {

    val argumentList: List<AtArgument>
    val funcName: AtFuncName
    val returnValue: AtReturnValue

    fun setArgumentList(argumentList: String)
    fun setReturnValue(returnValue: String)
    fun setFunction(function: String)
}
