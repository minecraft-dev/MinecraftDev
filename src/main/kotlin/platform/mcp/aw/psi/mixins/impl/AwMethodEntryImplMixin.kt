/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.aw.gen.psi.AwTypes
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwMethodEntryMixin
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class AwMethodEntryImplMixin(node: ASTNode) : AwEntryImplMixin(node), AwMethodEntryMixin {
    override val methodName: String?
        get() = findChildByType<PsiElement>(AwTypes.MEMBER_NAME)?.text

    override val methodDescriptor: String?
        get() = findChildByType<PsiElement>(AwTypes.METHOD_DESC)?.text
}
