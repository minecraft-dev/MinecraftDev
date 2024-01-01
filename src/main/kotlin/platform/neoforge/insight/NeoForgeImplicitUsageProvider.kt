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

package com.demonwav.mcdev.platform.neoforge.insight

import com.demonwav.mcdev.platform.neoforge.util.NeoForgeConstants
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod

class NeoForgeImplicitUsageProvider : ImplicitUsageProvider {

    override fun isImplicitUsage(element: PsiElement) = isNetworkMessageOrHandler(element)

    private fun isNetworkMessageOrHandler(element: PsiElement): Boolean {
        if (element !is PsiMethod || element.isConstructor && element.hasParameters()) {
            return false
        }

        val containingClass = element.containingClass ?: return false
        return containingClass.extendsOrImplements(NeoForgeConstants.NETWORK_MESSAGE)
    }

    override fun isImplicitRead(element: PsiElement) = false
    override fun isImplicitWrite(element: PsiElement) = false
}
