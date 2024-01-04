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

package com.demonwav.mcdev.inspection

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement

class StaticListenerInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != "MethodMayBeStatic") {
            return false
        }

        val method = element.findContainingMethod() ?: return false

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        val facet = MinecraftFacet.getInstance(module) ?: return false

        return facet.suppressStaticListener(method)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY
}
