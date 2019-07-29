/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.inspection

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement

class StaticListenerInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != "MethodMayBeStatic") {
            return false
        }

        val method = element.findContainingMethod() ?: return false

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        val facet = MinecraftFacet.getInstance(module) ?: return false

        return facet.suppressStaticListener(method)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
