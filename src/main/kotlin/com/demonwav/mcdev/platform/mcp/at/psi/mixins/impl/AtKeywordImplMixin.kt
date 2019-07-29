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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtKeywordMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtKeywordImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtKeywordMixin {

    override val keywordValue
        get() = AtElementFactory.Keyword.match(keywordElement.text)!!

    override fun setKeyword(keyword: AtElementFactory.Keyword) {
        replace(AtElementFactory.createKeyword(project, keyword))
    }
}
