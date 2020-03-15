/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.implements.suppress

import com.demonwav.mcdev.platform.mixin.util.isSoftImplementedMethod
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.codeInspection.visibility.VisibilityInspection
import com.intellij.psi.PsiElement

class SoftImplementInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != VisibilityInspection.SHORT_NAME && toolId != "override") {
            return false
        }

        val method = element.findContainingMethod() ?: return false
        return method.isSoftImplementedMethod()
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
