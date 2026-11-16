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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.parentOfType

class MixinInnerClassInspection : MixinInspection() {

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            val outerClass = psiClass.containingClass ?: return
            if (!outerClass.isMixin) {
                if (outerClass is PsiAnonymousClass && outerClass.parentOfType<PsiClass>()?.isMixin == true) {
                    holder.registerProblem(
                        psiClass.nameIdentifier ?: psiClass,
                        MCDevBundle("inspection.mixin_inner_class.inside_anonymous"),
                    )
                }

                return
            }

            // Static inner classes are fine if they are also Mixins
            if (psiClass.isMixin) {
                // Ensure inner class is static
                if (!psiClass.hasModifierProperty(PsiModifier.STATIC)) {
                    holder.registerProblem(
                        psiClass.modifierList!!,
                        MCDevBundle("inspection.mixin_inner_class.must_be_static"),
                        QuickFixFactory.getInstance().createModifierListFix(psiClass, PsiModifier.STATIC, true, false),
                    )
                }
            }
        }

        override fun visitAnonymousClass(psiClass: PsiAnonymousClass) {
            val outerClass = psiClass.parent?.findContainingClass() ?: return
            if (outerClass !is PsiAnonymousClass) {
                return
            }
            if (outerClass.parentOfType<PsiClass>()?.isMixin != true) {
                return
            }

            holder.registerProblem(
                psiClass.baseClassReference,
                MCDevBundle("inspection.mixin_inner_class.double_nested_anonymous")
            )
        }
    }
}
