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

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.FINAL
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MUTABLE
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression

class ShadowFinalInspection : MixinInspection() {

    override fun getStaticDescription() =
        "@Final annotated fields cannot be modified, as the field it is targeting is final. " +
            "This can be overridden with @Mutable."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAssignmentExpression(expression: PsiAssignmentExpression) {
            val left = expression.lExpression as? PsiReferenceExpression ?: return
            val resolved = left.resolve() as? PsiModifierListOwner ?: return
            val modifiers = resolved.modifierList ?: return

            if (modifiers.findAnnotation(FINAL) != null && modifiers.findAnnotation(MUTABLE) == null) {
                holder.registerProblem(
                    expression,
                    "@Final fields cannot be modified",
                    AddAnnotationFix(MUTABLE, resolved),
                )
            }
        }
    }
}
