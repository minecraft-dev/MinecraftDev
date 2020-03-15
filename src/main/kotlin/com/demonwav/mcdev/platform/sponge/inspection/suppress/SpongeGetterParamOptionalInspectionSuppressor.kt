/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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

        return param.hasAnnotation(SpongeConstants.GETTER_ANNOTATION)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<out SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        const val INSPECTION = "OptionalUsedAsFieldOrParameterType"
    }
}
