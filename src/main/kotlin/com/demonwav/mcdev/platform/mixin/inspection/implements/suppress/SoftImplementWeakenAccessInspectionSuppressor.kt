/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.implements.suppress

import com.demonwav.mcdev.platform.mixin.util.isSoftImplementedMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.visibility.VisibilityInspection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiMethod

class SoftImplementWeakenAccessInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != VisibilityInspection.SHORT_NAME || element !is PsiKeyword) {
            return false
        }

        val method = element.parent?.parent as? PsiMethod ?: return false
        return method.isSoftImplementedMethod()
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> = SuppressQuickFix.EMPTY_ARRAY
}
