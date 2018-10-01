/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.color

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

data class McpColorResult<out A>(
    val expression: PsiElement,
    val param: McpColorMethod.Param,
    val arg: A,
    val argRange: TextRange = expression.textRange
) {
    fun <A> withArg(arg: A) = McpColorResult(expression, param, arg, argRange)
}
