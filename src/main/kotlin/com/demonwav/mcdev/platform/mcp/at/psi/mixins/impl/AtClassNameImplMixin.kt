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
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtClassNameMixin
import com.demonwav.mcdev.util.findQualifiedClass
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtClassNameImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtClassNameMixin {

    override val classNameValue
        get() = findQualifiedClass(project, classNameText)

    override val classNameText: String
        get() = classNameElement.text

    override fun setClassName(className: String) {
        replace(AtElementFactory.createClassName(project, className))
    }
}
