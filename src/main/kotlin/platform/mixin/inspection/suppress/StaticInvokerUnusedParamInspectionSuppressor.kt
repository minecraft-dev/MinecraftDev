/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter

class StaticInvokerUnusedParamInspectionSuppressor : InspectionSuppressor {
    companion object {
        private const val INSPECTION = "unused"
    }

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        val parent = element.parent as? PsiParameter ?: return false
        if (!(element equivalentTo parent.nameIdentifier)) {
            return false
        }

        val method = parent.findContainingMethod() ?: return false
        if (!method.hasModifierProperty(PsiModifier.STATIC)) {
            return false
        }

        if (!method.hasAnnotation(MixinConstants.Annotations.INVOKER)) {
            return false
        }

        val clazz = method.findContainingClass() ?: return false
        if (!clazz.isMixin) {
            return false
        }

        return true
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
