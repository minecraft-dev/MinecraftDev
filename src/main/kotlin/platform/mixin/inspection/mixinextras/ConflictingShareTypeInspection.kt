/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.ShareUtil
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isLocalRef
import com.demonwav.mcdev.platform.mixin.util.unwrapLocalRef
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType

class ConflictingShareTypeInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.MixinExtras.SHARE)) {
                return
            }

            val mixinClass = annotation.findContainingClass() ?: return
            val expectedType = getShareType(annotation) ?: return
            val differentTypes = mutableSetOf<PsiType>()
            val differentTypeLocations = mutableSetOf<String>()

            val allShares = ShareUtil.getInstance(holder.project).getShares(annotation)
            for (otherShare in allShares) {
                if (otherShare equivalentTo annotation) {
                    continue
                }

                val otherType = getShareType(otherShare) ?: continue
                if (otherType != expectedType) {
                    differentTypes += otherType

                    val containingMethod = otherShare.findContainingMethod()
                    val containingClass = otherShare.findContainingClass()
                    if (containingClass != null && containingClass != mixinClass) {
                        differentTypeLocations += "class '${containingClass.fullQualifiedName}'"
                    } else if (containingMethod != null) {
                        differentTypeLocations += "method '${containingMethod.name}'"
                    }
                }
            }

            if (differentTypes.isNotEmpty()) {
                val differentTypesStr = differentTypes.joinToString(limit = 3) { it.presentableText }
                val differentLocationsStr = differentTypeLocations.joinToString(limit = 3)
                holder.registerProblem(
                    (annotation.parent?.parent as? PsiParameter)?.typeElement ?: annotation,
                    "@Share type ${expectedType.presentableText} is not compatible with other @Share types $differentTypesStr, found in $differentLocationsStr"
                )
            }
        }
    }

    private fun getShareType(share: PsiAnnotation): PsiType? {
        val param = share.parent?.parent as? PsiParameter ?: return null
        val paramType = param.type
        if (paramType.isLocalRef) {
            return null
        }
        return paramType.unwrapLocalRef()
    }
}
