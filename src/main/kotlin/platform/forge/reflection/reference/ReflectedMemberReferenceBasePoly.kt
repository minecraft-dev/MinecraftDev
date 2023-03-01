/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.reflection.reference

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

abstract class ReflectedMemberReferenceBasePoly(element: PsiLiteral) : PsiReferenceBase.Poly<PsiLiteral>(element) {

    abstract val memberName: String

    protected fun findReferencedClass(): PsiClass? {
        val callParams = element.parent as? PsiExpressionList
        val classRef = callParams?.expressions?.first() as? PsiClassObjectAccessExpression
        val type = classRef?.operand?.type as? PsiClassType
        return type?.resolve()
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val typeClass = findReferencedClass() ?: return arrayOf()

        val name = memberName
        val srgManager = element.findModule()?.let { MinecraftFacet.getInstance(it) }
            ?.getModuleOfType(McpModuleType)?.srgManager
        val srgMap = srgManager?.srgMapNow
        val mcpName = srgMap?.mapMcpToSrgName(name) ?: name

        return typeClass.allFields.asSequence()
            .filter { it.name == mcpName }
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }
}
