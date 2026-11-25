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

package com.demonwav.mcdev.platform.minecraft.unpick.inspections

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickClassName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickMethodName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickPackageName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickVisitor
import com.demonwav.mcdev.platform.minecraft.unpick.psi.UnpickClassReference
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

class UnresolvedUnpickReferenceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : UnpickVisitor() {
            override fun visitClassName(element: UnpickClassName) {
                checkClassReferences(element)
            }

            override fun visitPackageName(element: UnpickPackageName) {
                checkClassReferences(element)
            }

            private fun checkClassReferences(element: PsiElement) {
                // Only report the first unresolved part, the rest can't resolve either
                val reference = element.references.filterIsInstance<UnpickClassReference>()
                    .firstOrNull { it.resolve() == null } ?: return
                val message = if (reference.isPackage) {
                    MCDevBundle("inspection.unpick_unresolved_reference.package", reference.targetName)
                } else {
                    MCDevBundle("inspection.unpick_unresolved_reference.class", reference.targetName)
                }
                holder.registerProblem(
                    element,
                    message,
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                    reference.rangeInElement
                )
            }

            override fun visitMethodName(element: UnpickMethodName) {
                val owner = element.ownerClassName ?: return
                if (owner.resolveClass() == null || element.multiResolve(false).isNotEmpty()) {
                    return
                }
                holder.registerProblem(
                    element as PsiElement,
                    MCDevBundle(
                        "inspection.unpick_unresolved_reference.method",
                        element.canonicalText,
                        element.descriptor ?: "",
                        owner.qualifiedName,
                    ),
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                )
            }

            override fun visitFieldName(element: UnpickFieldName) {
                val owner = element.ownerClassName ?: return
                if (owner.resolveClass() == null || element.resolveField() != null) {
                    return
                }
                holder.registerProblem(
                    element as PsiElement,
                    MCDevBundle("inspection.unpick_unresolved_reference.field", element.text, owner.qualifiedName),
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                )
            }
        }
}
