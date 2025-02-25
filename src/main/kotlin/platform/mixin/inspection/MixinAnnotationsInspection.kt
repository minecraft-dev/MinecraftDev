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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifierList

class MixinAnnotationsInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports Mixin annotations outside of @Mixin classes"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (MixinAnnotationHandler.forMixinAnnotation(annotation, annotation.project) == null) {
                return
            }

            // Annotation must be either on or in a Mixin class
            val containingClass =
                (annotation.owner as? PsiModifierList)?.parent as? PsiClass ?: annotation.findContainingClass()
                    ?: return
            if (!containingClass.isMixin) {
                holder.registerProblem(
                    annotation,
                    "@${annotation.nameReferenceElement?.text} can be only used in a @Mixin class",
                    RemoveAnnotationQuickFix(annotation, null),
                )
            }
        }
    }
}
