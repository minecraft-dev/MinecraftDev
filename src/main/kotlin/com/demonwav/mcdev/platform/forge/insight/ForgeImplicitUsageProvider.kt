/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.insight

import com.demonwav.mcdev.platform.forge.inspections.simpleimpl.SimpleImplUtil
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class ForgeImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement) = isCoreMod(element) || isNetworkMessageOrHandler(element)

    private fun isCoreMod(element: PsiElement): Boolean {
        return element is PsiClass && element.extendsOrImplements(ForgeConstants.CORE_MOD_INTERFACE)
    }

    private fun isNetworkMessageOrHandler(element: PsiElement): Boolean {
        if (element !is PsiMethod || element.isConstructor && element.hasParameters()) {
            return false
        }

        val containingClass = element.containingClass ?: return false
        return SimpleImplUtil.isMessageOrHandler(containingClass)
    }

    override fun isImplicitRead(element: PsiElement) = false
    override fun isImplicitWrite(element: PsiElement) = false
}
