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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtFieldNameMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtFieldNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtFieldNameMixin {

    override fun setFieldName(fieldName: String) {
        replace(AtElementFactory.createFieldName(project, fieldName))
    }

    override val fieldNameText: String
        get() = nameElement.text
}
