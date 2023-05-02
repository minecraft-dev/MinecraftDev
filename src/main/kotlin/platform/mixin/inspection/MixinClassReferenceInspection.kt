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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiTypeElement

class MixinClassReferenceInspection : MixinInspection() {

    override fun getStaticDescription() =
        "A Mixin class doesn't exist at runtime, and thus cannot be referenced directly."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitTypeElement(type: PsiTypeElement) {
            val classType = type.type as? PsiClassType ?: return
            val referencedClass = classType.resolve() ?: return

            if (!referencedClass.isMixin) {
                return
            }

            // Check if the the reference is a super Mixin
            val psiClass = type.findContainingClass() ?: return
            if (psiClass.isEquivalentTo(referencedClass) || psiClass.isInheritor(referencedClass, true)) {
                // Mixin class is part of the hierarchy
                return
            }

            // Check if the reference is an accessor Mixin
            if (referencedClass.isAccessorMixin) {
                return
            }

            holder.registerProblem(type, "Mixin class cannot be referenced directly")
        }
    }
}
