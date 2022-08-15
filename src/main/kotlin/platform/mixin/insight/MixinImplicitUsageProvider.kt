/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

class MixinImplicitUsageProvider : ImplicitUsageProvider {

    private fun isShadowField(element: PsiElement) = element is PsiField && element.hasAnnotation(SHADOW)

    private fun isParameterInShadow(element: PsiElement): Boolean {
        if (element !is PsiParameter) {
            return false
        }

        val method = element.declarationScope as? PsiMethod ?: return false
        return method.hasAnnotation(SHADOW)
    }

    override fun isImplicitUsage(element: PsiElement) = isParameterInShadow(element)
    override fun isImplicitRead(element: PsiElement) = isShadowField(element)
    override fun isImplicitWrite(element: PsiElement) = isShadowField(element)
}
