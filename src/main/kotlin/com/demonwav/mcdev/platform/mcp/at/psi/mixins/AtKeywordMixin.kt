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

import com.demonwav.mcdev.platform.mcp.at.AtElementFactory
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.intellij.psi.PsiElement

interface AtKeywordMixin : AtElement {

    val keywordElement: PsiElement
    val keywordValue: AtElementFactory.Keyword

    fun setKeyword(keyword: AtElementFactory.Keyword)
}
