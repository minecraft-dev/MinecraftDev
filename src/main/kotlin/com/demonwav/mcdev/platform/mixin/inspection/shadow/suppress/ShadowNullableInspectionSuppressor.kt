/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.shadow.suppress

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier

class ShadowNullableInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        // Skip nullable warnings for @Shadow fields
        if (toolId != "NullableProblems" || element !is PsiIdentifier) {
            return false
        }

        val field = element.parent as? PsiField ?: return false
        return field.modifierList?.findAnnotation(SHADOW) != null
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
