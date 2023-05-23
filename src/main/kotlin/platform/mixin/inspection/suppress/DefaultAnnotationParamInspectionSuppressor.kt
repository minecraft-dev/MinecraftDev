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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.CONSTANT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INJECT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_ARG
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_ARGS
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MODIFY_VARIABLE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.REDIRECT
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.parentOfType

class DefaultAnnotationParamInspectionSuppressor : InspectionSuppressor {
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (toolId != INSPECTION) {
            return false
        }

        val name = element.parentOfType<PsiNameValuePair>()?.attributeName ?: return false
        val annotation = element.parentOfType<PsiAnnotation>() ?: return false

        if (name in CONSTANT_SUPPRESSED && annotation.hasQualifiedName(CONSTANT)) {
            return true
        }

        if (name == "remap" && REMAP_SUPPRESSED.any(annotation::hasQualifiedName)) {
            val currentRemap = annotation.findAttributeValue("remap")?.constantValue as? Boolean
                ?: return false
            val parentRemap = generateSequence<PsiElement>(annotation) { elem ->
                elem.parent?.takeIf { elem !is PsiClass }
            }
                .filterIsInstance<PsiModifierListOwner>()
                .drop(1) // don't look at our own owner
                .mapNotNull { annotationOwner ->
                    REMAP_SUPPRESSED.mapFirstNotNull {
                        annotationOwner.findAnnotation(it)?.findDeclaredAttributeValue("remap")?.constantValue
                            as? Boolean
                    }
                }
                .firstOrNull() ?: true
            if (currentRemap != parentRemap) {
                return true
            }
        }

        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        private const val INSPECTION = "DefaultAnnotationParam"
        private val REMAP_SUPPRESSED = setOf(
            AT,
            INJECT,
            MODIFY_ARG,
            MODIFY_ARGS,
            MODIFY_VARIABLE,
            REDIRECT,
            ACCESSOR,
            INVOKER,
            MIXIN,
        )
        private val CONSTANT_SUPPRESSED = setOf(
            "intValue",
            "floatValue",
            "longValue",
            "doubleValue",
            "stringValue",
            "classValue",
        )
    }
}
