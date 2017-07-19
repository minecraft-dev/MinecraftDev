/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.folding

import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase
import com.intellij.psi.util.PsiFormatUtilBase.SHOW_CONTAINING_CLASS

class MixinFoldingBuilder : CustomFoldingBuilder() {

    // I'm not dumb
    override fun isDumbAware() = false

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        val settings = MixinFoldingSettings.instance.state
        return when (node.psi) {
            is PsiTypeCastExpression -> settings.foldObjectCasts
            is PsiAnnotationMemberValue -> settings.foldTargetDescriptors
            else -> true
        }
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        val element = node.psi
        when (element) {
            is PsiTypeCastExpression -> {
                val castText = element.castType?.text ?: return node.text
                return "($castText)"
            }
            is PsiAnnotationMemberValue -> {
                val value = element.constantStringValue ?: return node.text
                val member = MixinMemberReference.parse(value)?.resolveMember(element.project, element.resolveScope) ?: return node.text
                return when (member) {
                    is PsiMethod -> PsiFormatUtil.formatMethod(member, PsiSubstitutor.EMPTY,
                        PsiFormatUtilBase.SHOW_NAME or PsiFormatUtilBase.SHOW_PARAMETERS or SHOW_CONTAINING_CLASS,
                        PsiFormatUtilBase.SHOW_TYPE)
                    is PsiVariable -> PsiFormatUtil.formatVariable(member, PsiFormatUtilBase.SHOW_NAME or SHOW_CONTAINING_CLASS, PsiSubstitutor.EMPTY)
                    else -> member.presentation?.presentableText ?: node.text
                }
            }
        }

        return node.text
    }

    override fun buildLanguageFoldRegions(descriptors: MutableList<FoldingDescriptor>, root: PsiElement, document: Document, quick: Boolean) {
        if (root !is PsiJavaFile || !MixinModuleType.isInModule(root)) {
            return
        }

        root.accept(Visitor(descriptors))
    }

    private class Visitor(private val descriptors: MutableList<FoldingDescriptor>) : JavaRecursiveElementWalkingVisitor() {

        val settings = MixinFoldingSettings.instance.state

        override fun visitAnnotation(annotation: PsiAnnotation) {
            super.visitAnnotation(annotation)

            if (!settings.foldTargetDescriptors) {
                return
            }

            val qualifiedName = annotation.qualifiedName ?: return
            if (qualifiedName != AT) {
                return
            }

            val target = annotation.findDeclaredAttributeValue("target") ?: return
            descriptors.add(FoldingDescriptor(target, target.textRange))
        }

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

                descriptors.add(FoldingDescriptor(expression, TextRange(start.startOffset, end.startOffset + end.textLength)))
            }
        }

    }

}
