/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.shadow.suppress

import com.demonwav.mcdev.platform.mixin.util.isShadow
import com.demonwav.mcdev.util.findContainingMember
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.visibility.VisibilityInspection
import com.intellij.psi.PsiElement

class ShadowInspectionSuppressor : InspectionSuppressor {

    private val SUPPRESSED_INSPECTIONS = setOf("UnusedReturnValue", "SameParameterValue", "Guava", VisibilityInspection.SHORT_NAME)

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId !in SUPPRESSED_INSPECTIONS) {
            return false
        }

        val member = element.findContainingMember() ?: return false
        return member.isShadow
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> = SuppressQuickFix.EMPTY_ARRAY
}
