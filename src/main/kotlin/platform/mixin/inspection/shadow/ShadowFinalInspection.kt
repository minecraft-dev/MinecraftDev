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

package com.demonwav.mcdev.platform.mixin.inspection.shadow

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.FINAL
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MUTABLE
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.intention.AddAnnotationModCommandAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil

class ShadowFinalInspection : MixinInspection() {

    override fun getStaticDescription() =
        "@Final annotated fields cannot be modified, as the field it is targeting is final. " +
            "This can be overridden with @Mutable."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            if (!PsiUtil.isAccessedForWriting(expression)) {
                return
            }

            val field = expression.resolve() as? PsiField ?: return
            val modifiers = field.modifierList ?: return

            if (modifiers.hasAnnotation(FINAL) && !modifiers.hasAnnotation(MUTABLE)) {
                if (!isAssignmentInAppropriateInitializer(expression, field, modifiers)) {
                    holder.registerProblem(
                        expression,
                        "@Final fields cannot be modified",
                        LocalQuickFix.from(AddAnnotationModCommandAction(MUTABLE, field))!!,
                    )
                }
            }
        }

        private fun isAssignmentInAppropriateInitializer(
            expression: PsiReferenceExpression,
            field: PsiField,
            modifiers: PsiModifierList
        ): Boolean {
            if (field.containingClass != expression.findContainingClass()) {
                return false
            }

            val initializer = PsiTreeUtil.getParentOfType(
                expression,
                PsiClassInitializer::class.java,
                true,
                PsiClass::class.java,
                PsiMethod::class.java,
                PsiLambdaExpression::class.java,
            ) ?: return false

            return initializer.hasModifierProperty(PsiModifier.STATIC) == modifiers.hasModifierProperty(PsiModifier.STATIC)
        }
    }
}
