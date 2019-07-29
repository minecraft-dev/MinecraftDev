/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.suppress

import com.demonwav.mcdev.platform.mixin.util.isOverwrite
import com.demonwav.mcdev.platform.mixin.util.isShadow
import com.demonwav.mcdev.util.findContainingMember
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.visibility.VisibilityInspection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class ShadowOverwriteInspectionSuppressor : InspectionSuppressor {

    private val SUPPRESSED_INSPECTIONS = setOf(
        "UnusedReturnValue", "SameParameterValue", "Guava", VisibilityInspection.SHORT_NAME,
        "MethodMayBeStatic"
    )

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId !in SUPPRESSED_INSPECTIONS) {
            return false
        }

        val member = element.findContainingMember() ?: return false
        return member.isShadow || (member is PsiMethod && member.isOverwrite)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
