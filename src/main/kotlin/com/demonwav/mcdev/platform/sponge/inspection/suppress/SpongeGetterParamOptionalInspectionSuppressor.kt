/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection.suppress

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.util.parentOfType

class SpongeGetterParamOptionalInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION || element !is PsiTypeElement) {
            return false
        }

        val param = element.parentOfType(PsiParameter::class) ?: return false
        val method = param.findContainingMethod() ?: return false
        if (!method.isValidSpongeListener()) {
            return false
        }

        val getterAnnotation = param.getAnnotation(SpongeConstants.GETTER_ANNOTATION) ?: return false
        val targetMethod = getterAnnotation.resolveSpongeGetterTarget() ?: return false

        if (targetMethod.returnType != param.type) {
            return false
        }

        val parameterType = (param.type as JvmReferenceType).resolve()
        return parameterType != null && (parameterType as PsiClass).qualifiedName == SpongeConstants.OPTIONAL
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> = SuppressQuickFix.EMPTY_ARRAY

    companion object {
        const val INSPECTION = "OptionalUsedAsFieldOrParameterType"
    }
}
