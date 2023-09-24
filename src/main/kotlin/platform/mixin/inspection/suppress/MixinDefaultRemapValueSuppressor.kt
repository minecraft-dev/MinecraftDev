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

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType

/**
 * Suppresses warnings about `remap = true` in `@At`s if the surrounding injector has `remap = false`
 */
class MixinDefaultRemapValueSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        if (element.constantValue == true) {
            val at = element.parentOfType<PsiAnnotation>()?.takeIf { it.qualifiedName == MixinConstants.Annotations.AT }
                ?: return false
            val injector = at.parentsOfType<PsiAnnotation>()
                .filter { ann ->
                    ann.qualifiedName
                        ?.let { MixinAnnotationHandler.forMixinAnnotation(it) is InjectorAnnotationHandler } == true
                }
                .firstOrNull() ?: return false
            if (injector.findAttributeValue("remap")?.constantValue == false) {
                return true
            }
        }

        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    private companion object {
        private const val INSPECTION = "DefaultAnnotationParam"
    }
}
