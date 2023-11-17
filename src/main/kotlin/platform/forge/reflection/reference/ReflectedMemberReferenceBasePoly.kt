/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
            ?.getModuleOfType(McpModuleType)?.mappingsManager
        val srgMap = srgManager?.mappingsNow
        val mcpName = srgMap?.mapIntermediaryToMapped(name) ?: name

        return typeClass.allFields.asSequence()
            .filter { it.name == mcpName }
            .map(::PsiElementResolveResult)
            .toTypedArray()
    }
}
