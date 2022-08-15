/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.suppress

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.DYNAMIC
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findContainingMember
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement

class DynamicInspectionSuppressor : InspectionSuppressor {
    private val suppressedInspections = setOf(
        "AmbiguousMixinReference",
        "InvalidInjectorMethodSignature",
        "InvalidMemberReference",
        "MixinAnnotationTarget",
        "OverwriteModifiers",
        "ShadowModifiers",
        "UnqualifiedMemberReference",
        "UnnecessaryQualifiedMemberReference",
        "UnresolvedMixinReference"
    )

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId !in suppressedInspections) {
            return false
        }
        return element.findContainingMember()?.findAnnotation(DYNAMIC) != null
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }
}
