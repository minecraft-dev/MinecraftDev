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

package com.demonwav.mcdev.platform.forge.insight

import com.demonwav.mcdev.platform.forge.inspections.simpleimpl.SimpleImplUtil
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class ForgeImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement) = isCoreMod(element) || isNetworkMessageOrHandler(element)

    private fun isCoreMod(element: PsiElement): Boolean {
        return element is PsiClass && element.extendsOrImplements(ForgeConstants.CORE_MOD_INTERFACE)
    }

    private fun isNetworkMessageOrHandler(element: PsiElement): Boolean {
        if (element !is PsiMethod || element.isConstructor && element.hasParameters()) {
            return false
        }

        val containingClass = element.containingClass ?: return false
        return SimpleImplUtil.isMessageOrHandler(containingClass)
    }

    override fun isImplicitRead(element: PsiElement) = false
    override fun isImplicitWrite(element: PsiElement) = false
}
