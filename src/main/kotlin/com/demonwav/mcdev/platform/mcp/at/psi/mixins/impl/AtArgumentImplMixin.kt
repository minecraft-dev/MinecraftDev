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
import com.demonwav.mcdev.platform.mcp.at.psi.getClassFromString
import com.demonwav.mcdev.platform.mcp.at.psi.mixins.AtArgumentMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class AtArgumentImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AtArgumentMixin {

    override val argumentClass
        get() = getClassFromString(argumentText, project)

    override val argumentText: String
        get() = classValue?.text ?: primitive!!.text

    override fun setArgument(argument: String) {
        replace(AtElementFactory.createArgument(project, argument))
    }
}
