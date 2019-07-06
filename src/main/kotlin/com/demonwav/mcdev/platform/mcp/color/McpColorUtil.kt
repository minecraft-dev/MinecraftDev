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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import java.awt.Color

fun PsiElement.findColors(): List<McpColorResult<Color>> {
    if (this !is PsiMethodCallExpression) {
        return emptyList()
    }

    val method = McpColorMethods[this].find { it.match(this) } ?: return emptyList()

    return method.extractColors(this)
}
