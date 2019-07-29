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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtEntryMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtEntryImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtEntryMixin {

    override fun setEntry(entry: String) {
        replace(AtElementFactory.createEntry(project, entry))
    }

    override fun setKeyword(keyword: AtElementFactory.Keyword) {
        this.keyword.replace(AtElementFactory.createKeyword(project, keyword))
    }

    override fun setClassName(className: String) {
        this.className.replace(AtElementFactory.createClassName(project, className))
    }

    override fun setFieldName(fieldName: String) {
        val newField = AtElementFactory.createFieldName(project, fieldName)
        replaceMember(newField)
    }

    override fun setFunction(function: String) {
        val atFunction = AtElementFactory.createFunction(project, function)
        replaceMember(atFunction)
    }

    override fun setAsterisk() {
        val asterisk = AtElementFactory.createAsterisk(project)
        replaceMember(asterisk)
    }
}
