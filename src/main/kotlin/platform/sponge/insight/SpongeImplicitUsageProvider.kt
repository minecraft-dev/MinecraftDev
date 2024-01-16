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

package com.demonwav.mcdev.platform.sponge.insight

import com.demonwav.mcdev.platform.sponge.util.isInSpongePluginClass
import com.demonwav.mcdev.platform.sponge.util.isInjected
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class SpongeImplicitUsageProvider : ImplicitUsageProvider {
    override fun isImplicitWrite(element: PsiElement): Boolean = isPluginClassInjectedField(element, false)

    override fun isImplicitRead(element: PsiElement): Boolean = false

    override fun isImplicitUsage(element: PsiElement): Boolean =
        isPluginClassEmptyConstructor(element) || isPluginClassInjectedSetter(element)

    override fun isImplicitlyNotNullInitialized(element: PsiElement): Boolean =
        isPluginClassInjectedField(element, true)

    private fun isPluginClassEmptyConstructor(element: PsiElement): Boolean =
        element is PsiMethod && element.isInSpongePluginClass() && element.isConstructor && !element.hasParameters()

    private fun isPluginClassInjectedField(element: PsiElement, optionalSensitive: Boolean): Boolean =
        element is PsiField && element.isInSpongePluginClass() && isInjected(element, optionalSensitive)

    private fun isPluginClassInjectedSetter(element: PsiElement): Boolean =
        element is PsiMethod && element.isInSpongePluginClass() && isInjected(element, false)
}
