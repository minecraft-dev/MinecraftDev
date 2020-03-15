/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.psi.mixins

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtArgument
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFuncName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtReturnValue
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement

interface AtFunctionMixin : AtElement {

    val argumentList: List<AtArgument>
    val funcName: AtFuncName
    val returnValue: AtReturnValue

    fun setArgumentList(argumentList: String)
    fun setReturnValue(returnValue: String)
    fun setFunction(function: String)
}
