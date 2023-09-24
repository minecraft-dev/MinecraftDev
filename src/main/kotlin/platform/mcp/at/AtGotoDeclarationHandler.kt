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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFuncName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtTypes
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.getPrimitiveType
import com.demonwav.mcdev.util.parseClassDescriptor
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

class AtGotoDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor,
    ): Array<out PsiElement>? {
        if (sourceElement?.language !== AtLanguage) {
            return null
        }

        val module = ModuleUtilCore.findModuleForPsiElement(sourceElement) ?: return null

        val instance = MinecraftFacet.getInstance(module) ?: return null

        val mcpModule = instance.getModuleOfType(McpModuleType) ?: return null

        val srgMap = mcpModule.mappingsManager?.mappingsNow ?: return null

        return when {
            sourceElement.node.treeParent.elementType === AtTypes.CLASS_NAME -> {
                val className = sourceElement.parent as AtClassName
                val classSrgToMcp = srgMap.getMappedClass(className.classNameText)
                val psiClass = findQualifiedClass(sourceElement.project, classSrgToMcp) ?: return null
                arrayOf(psiClass)
            }
            sourceElement.node.treeParent.elementType === AtTypes.FUNC_NAME -> {
                val funcName = sourceElement.parent as AtFuncName
                val function = funcName.parent as AtFunction
                val entry = function.parent as AtEntry

                val reference = srgMap.getMappedMethod(AtMemberReference.get(entry, function) ?: return null)
                val member = reference.resolveMember(sourceElement.project) ?: return null
                arrayOf(member)
            }
            sourceElement.node.treeParent.elementType === AtTypes.FIELD_NAME -> {
                val fieldName = sourceElement.parent as AtFieldName
                val entry = fieldName.parent as AtEntry

                val reference = srgMap.getMappedField(AtMemberReference.get(entry, fieldName) ?: return null)
                val member = reference.resolveMember(sourceElement.project) ?: return null
                arrayOf(member)
            }
            sourceElement.node.elementType === AtTypes.CLASS_VALUE -> {
                val className = srgMap.getMappedClass(parseClassDescriptor(sourceElement.text))
                val psiClass = findQualifiedClass(sourceElement.project, className) ?: return null
                arrayOf(psiClass)
            }
            sourceElement.node.elementType === AtTypes.PRIMITIVE -> {
                val text = sourceElement.text
                if (text.length != 1) {
                    return null
                }

                val type = getPrimitiveType(text[0]) ?: return null

                val boxedType = type.boxedTypeName ?: return null

                val psiClass = JavaPsiFacade.getInstance(sourceElement.project).findClass(
                    boxedType,
                    GlobalSearchScope.allScope(sourceElement.project),
                ) ?: return null
                arrayOf(psiClass)
            }
            else -> null
        }
    }

    override fun getActionText(context: DataContext): String? = null
}
