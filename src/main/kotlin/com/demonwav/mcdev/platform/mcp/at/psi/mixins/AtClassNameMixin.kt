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

import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

interface AtClassNameMixin : AtElement {

    val classNameElement: PsiElement
    val classNameValue: PsiClass?
    val classNameText: String

    fun setClassName(className: String)
}
