/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.sideonly

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.google.common.base.Strings
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.search.GlobalSearchScope

class SidedProxyAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiField) {
            return
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        val instance = MinecraftFacet.getInstance(module) ?: return

        if (!instance.isOfType(ForgeModuleType)) {
            return
        }

        check(element)
    }

    companion object {
        fun check(field: PsiField) {
            val modifierList = field.modifierList ?: return

            val annotation = modifierList.findAnnotation(ForgeConstants.SIDED_PROXY_ANNOTATION) ?: return

            val clientSide = annotation.findAttributeValue("clientSide")
            val serverSide = annotation.findAttributeValue("serverSide")

            if (clientSide != null && !Strings.isNullOrEmpty(clientSide.text)) {
                annotateClass(clientSide, Side.CLIENT)
            }

            if (serverSide != null && !Strings.isNullOrEmpty(serverSide.text)) {
                annotateClass(serverSide, Side.SERVER)
            }
        }

        private fun annotateClass(value: PsiAnnotationMemberValue, side: Side) {
            val text: String?
            if (value is PsiLiteralExpressionImpl) {

                text = value.innerText
                if (text == null) {
                    return
                }
            } else if (value is PsiReferenceExpression) {

                val resolve = value.resolve() as? PsiField ?: return

                text = JavaConstantExpressionEvaluator.computeConstantExpression(
                    resolve.initializer,
                    null,
                    false
                ) as? String ?: return
            } else {
                return
            }

            val psiClass =
                JavaPsiFacade.getInstance(value.project).findClass(text, GlobalSearchScope.allScope(value.project))
                    ?: return

            psiClass.putUserData(Side.KEY, side)
        }
    }
}
