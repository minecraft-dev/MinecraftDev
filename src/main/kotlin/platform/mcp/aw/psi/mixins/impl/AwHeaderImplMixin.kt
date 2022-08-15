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
import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwHeaderMixin
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

abstract class AwHeaderImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), AwHeaderMixin {

    override val versionString: String?
        get() = findChildByType<PsiElement>(AwTypes.HEADER_VERSION_ELEMENT)?.text

    override val namespaceString: String?
        get() = findChildByType<PsiElement>(AwTypes.HEADER_NAMESPACE_ELEMENT)?.text
}
