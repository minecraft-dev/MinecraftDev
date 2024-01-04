/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
        "UnresolvedMixinReference",
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
