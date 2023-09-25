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

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.CONSTANT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
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
import com.intellij.psi.util.parentsOfType

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

        if (name == "remap" && annotation.hasRemap) {
            val currentRemap = annotation.findAttributeValue("remap")?.constantValue as? Boolean
                ?: return false
            var parents = annotation.parentsOfType<PsiAnnotation>(withSelf = false).filter { it.hasRemap }
            parents += generateSequence<PsiElement>(annotation) { elem ->
                elem.parent?.takeIf { elem !is PsiClass }
            }
                .filterIsInstance<PsiModifierListOwner>()
                .drop(1) // don't look at our own owner
                .mapNotNull { annotationOwner ->
                    HAS_REMAP.mapFirstNotNull {
                        annotationOwner.findAnnotation(it)
                    }
                }
            val parentRemap = parents.firstNotNullOfOrNull {
                it.findDeclaredAttributeValue("remap")?.constantValue as? Boolean
            } ?: true
            if (currentRemap != parentRemap) {
                return true
            }
        }

        return false
    }

    private val PsiAnnotation.hasRemap get() = qualifiedName?.let { it in HAS_REMAP } == true

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> =
        SuppressQuickFix.EMPTY_ARRAY

    companion object {
        private const val INSPECTION = "DefaultAnnotationParam"
        private val HAS_REMAP = buildSet {
            add(MIXIN)
            add(AT)
            add(ACCESSOR)
            add(INVOKER)
            add(OVERWRITE)
            add(SHADOW)
            addAll(MixinAnnotationHandler.getBuiltinHandlers().map { it.first })
        }
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
