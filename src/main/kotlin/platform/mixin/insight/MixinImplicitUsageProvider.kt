/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.insight

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.isMixinExtrasSugar
import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter

class MixinImplicitUsageProvider : ImplicitUsageProvider {

    private fun isShadowField(element: PsiElement) = element is PsiField && element.hasAnnotation(SHADOW)

    private fun isParameterInShadow(element: PsiElement): Boolean {
        if (element !is PsiParameter) {
            return false
        }

        val method = element.declarationScope as? PsiMethod ?: return false
        return method.hasAnnotation(SHADOW)
    }

    private fun isHandlerImplicitlyUsed(element: PsiElement): Boolean {
        if (element is PsiParameter) {
            if (element.isMixinExtrasSugar) {
                return false
            }

            val declarationScope = element.declarationScope
            return if (declarationScope is PsiMethod) {
                isHandlerImplicitlyUsed(declarationScope)
            } else {
                false
            }
        }

        if (element is PsiModifierListOwner) {
            return element.annotations.any {
                MixinAnnotationHandler.forMixinAnnotation(it)?.isImplicitlyUsed == true
            }
        }

        return false
    }

    private fun isMixinAddedEnumConstant(element: PsiElement): Boolean {
        return element is PsiEnumConstant && element.containingClass?.isMixin == true
    }

    private fun isAbstractShadowInEnum(element: PsiElement): Boolean {
        return element is PsiMethod && element.hasModifierProperty(PsiModifier.ABSTRACT) && element.hasAnnotation(SHADOW) && element.containingClass?.isEnum == true
    }

    override fun isImplicitUsage(element: PsiElement) =
        isParameterInShadow(element) || isHandlerImplicitlyUsed(element) || isMixinAddedEnumConstant(element) || isAbstractShadowInEnum(element)
    override fun isImplicitRead(element: PsiElement) = isShadowField(element)
    override fun isImplicitWrite(element: PsiElement) = isShadowField(element)
}
