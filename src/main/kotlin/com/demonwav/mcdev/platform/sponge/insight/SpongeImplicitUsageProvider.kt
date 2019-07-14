/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.insight

import com.demonwav.mcdev.platform.sponge.util.isInSpongePluginClass
import com.demonwav.mcdev.platform.sponge.util.isInjected
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class SpongeImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitWrite(element: PsiElement): Boolean = isPluginClassInjectedField(element, false)

    override fun isImplicitRead(element: PsiElement): Boolean = false

    override fun isImplicitUsage(element: PsiElement): Boolean =
        isPluginClassEmptyConstructor(element) || isPluginClassInjectedSetter(element)

    override fun isImplicitlyNotNullInitialized(element: PsiElement): Boolean =
        isPluginClassInjectedField(element, true)

    private fun isPluginClassEmptyConstructor(element: PsiElement): Boolean =
        element is PsiMethod && element.isInSpongePluginClass() && element.isConstructor && !element.hasParameters()

    private fun isPluginClassInjectedField(element: PsiElement, optionalSensitive: Boolean): Boolean =
        element is PsiField && element.isInSpongePluginClass() && isInjected(element, optionalSensitive)

    private fun isPluginClassInjectedSetter(element: PsiElement): Boolean =
        element is PsiMethod && element.isInSpongePluginClass() && isInjected(element, false)
}
