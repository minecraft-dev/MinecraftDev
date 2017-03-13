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

import com.intellij.psi.PsiElement

interface AtFieldNameMixin : PsiElement {

    val nameElement: PsiElement
    val fieldNameText: String

    fun setFieldName(fieldName: String)
}
