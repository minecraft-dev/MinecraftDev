/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isShadow
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class MixinImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement) = element is PsiMethod && MixinConstants.Annotations.ENTRY_POINTS.any {
        element.findAnnotation(it) != null
    }

    private fun isShadow(element: PsiElement) = element is PsiField && element.isShadow

    override fun isImplicitRead(element: PsiElement) = isShadow(element)
    override fun isImplicitWrite(element: PsiElement) = isShadow(element)
}
