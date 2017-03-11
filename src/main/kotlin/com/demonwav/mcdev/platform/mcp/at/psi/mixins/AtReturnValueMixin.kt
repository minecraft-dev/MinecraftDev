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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

interface AtReturnValueMixin : PsiElement {

    val classValue: PsiElement?
    val primitive: PsiElement?
    val returnValueClass: PsiClass?
    val returnValueText: String

    fun setReturnValue(returnValue: String)
}
