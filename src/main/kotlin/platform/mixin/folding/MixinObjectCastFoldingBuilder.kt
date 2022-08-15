/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.folding

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.CompositeElement

class MixinObjectCastFoldingBuilder : CustomFoldingBuilder() {

    // I'm not dumb
    override fun isDumbAware() = false

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean =
        MixinFoldingSettings.instance.state.foldObjectCasts

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String? {
        val element = node.psi as? PsiTypeCastExpression ?: return null
        return "(${element.castType?.text ?: return node.text})"
    }

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

        val settings = MixinFoldingSettings.instance.state

        override fun visitTypeCastExpression(expression: PsiTypeCastExpression) {
            super.visitTypeCastExpression(expression)

            if (!settings.foldObjectCasts) {
                return
            }

            val innerCast = expression.operand as? PsiTypeCastExpression ?: return
            if ((innerCast.type as? PsiClassType)?.resolve()?.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                // Fold the two casts

                val start = (expression as? CompositeElement)?.findChildByRole(ChildRole.LPARENTH) ?: return
                val end = (innerCast as? CompositeElement)?.findChildByRole(ChildRole.RPARENTH) ?: return

                descriptors.add(
                    FoldingDescriptor(
                        expression.node,
                        TextRange(start.startOffset, end.startOffset + end.textLength)
                    )
                )
            }
        }
    }
}
