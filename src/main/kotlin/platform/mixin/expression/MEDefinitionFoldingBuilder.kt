/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.folding.MixinFoldingSettings
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiModifierList
import com.intellij.psi.util.PsiTreeUtil

class MEDefinitionFoldingBuilder : CustomFoldingBuilder() {
    override fun isDumbAware() = false

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean =
        MixinFoldingSettings.instance.state.foldDefinitions

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange) = "..."

    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean
    ) {
        if (root !is PsiJavaFile || !MixinModuleType.isInModule(root)) {
            return
        }

        root.accept(Visitor(descriptors))
    }

    private class Visitor(private val descriptors: MutableList<FoldingDescriptor>) :
        JavaRecursiveElementWalkingVisitor() {
        override fun visitModifierList(list: PsiModifierList) {
            val currentDefinitionList = mutableListOf<PsiAnnotation>()
            val definitionLists = mutableListOf<List<PsiAnnotation>>()

            for (annotation in list.annotations) {
                if (annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)) {
                    currentDefinitionList += annotation
                } else if (currentDefinitionList.isNotEmpty()) {
                    definitionLists += currentDefinitionList.toList()
                    currentDefinitionList.clear()
                }
            }

            if (currentDefinitionList.isNotEmpty()) {
                definitionLists += currentDefinitionList
            }

            if (definitionLists.isEmpty()) {
                return
            }

            for (definitionList in definitionLists) {
                val range = TextRange(
                    definitionList.first().parameterList.firstChild.nextSibling.textRange.startOffset,
                    PsiTreeUtil.getDeepestVisibleLast(definitionList.last())!!.textRange.startOffset,
                )
                descriptors.add(FoldingDescriptor(list.node, range))
            }
        }
    }
}
