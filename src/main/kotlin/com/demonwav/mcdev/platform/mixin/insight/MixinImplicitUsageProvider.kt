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

import com.demonwav.mcdev.platform.mixin.util.isShadow
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField

class MixinImplicitUsageProvider : ImplicitUsageProvider {

    // Handled in MixinEntryPoint
    override fun isImplicitUsage(element: PsiElement) = false

    private fun isShadowField(element: PsiElement) = element is PsiField && element.isShadow

    override fun isImplicitRead(element: PsiElement) = isShadowField(element)
    override fun isImplicitWrite(element: PsiElement) = isShadowField(element)
}
