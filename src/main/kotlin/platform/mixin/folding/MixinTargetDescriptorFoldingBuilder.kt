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
import com.demonwav.mcdev.platform.mixin.reference.target.TargetReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase.SHOW_CONTAINING_CLASS
import com.intellij.psi.util.PsiFormatUtilBase.SHOW_NAME
import com.intellij.psi.util.PsiFormatUtilBase.SHOW_PARAMETERS
import com.intellij.psi.util.PsiFormatUtilBase.SHOW_TYPE

class MixinTargetDescriptorFoldingBuilder : CustomFoldingBuilder() {

    override fun isDumbAware(): Boolean = false

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean =
        MixinFoldingSettings.instance.state.foldTargetDescriptors

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String? {
        val element = node.psi
        return TargetReference.resolveTarget(element)?.let { formatElement(it) }
    }

    private fun formatElement(element: PsiElement): String? {
        return when (element) {
            is PsiMethod -> {
                val options = SHOW_NAME or SHOW_PARAMETERS or SHOW_CONTAINING_CLASS
                PsiFormatUtil.formatMethod(element, PsiSubstitutor.EMPTY, options, SHOW_TYPE)
            }
            is PsiVariable -> {
                val options = SHOW_NAME or SHOW_CONTAINING_CLASS
                PsiFormatUtil.formatVariable(element, options, PsiSubstitutor.EMPTY)
            }
            is NavigationItem -> element.presentation?.presentableText
            else -> null
        }
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

        override fun visitAnnotation(annotation: PsiAnnotation) {
            super.visitAnnotation(annotation)

            if (!settings.foldTargetDescriptors) {
                return
            }

            val qualifiedName = annotation.qualifiedName ?: return
            if (qualifiedName != MixinConstants.Annotations.AT) {
                return
            }

            val target = annotation.findDeclaredAttributeValue("target") ?: return
            descriptors.add(FoldingDescriptor(target, target.textRange))
        }
    }
}
