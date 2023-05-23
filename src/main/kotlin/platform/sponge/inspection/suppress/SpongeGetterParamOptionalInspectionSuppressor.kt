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

package com.demonwav.mcdev.platform.sponge.inspection.suppress

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.parentOfTypes

class SpongeGetterParamOptionalInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION || element !is PsiTypeElement) {
            return false
        }

        val param = element.parentOfTypes(PsiParameter::class) ?: return false
        val method = param.findContainingMethod() ?: return false
        if (!method.isValidSpongeListener()) {
            return false
        }

        return param.hasAnnotation(SpongeConstants.GETTER_ANNOTATION)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        const val INSPECTION = "OptionalUsedAsFieldOrParameterType"
    }
}
