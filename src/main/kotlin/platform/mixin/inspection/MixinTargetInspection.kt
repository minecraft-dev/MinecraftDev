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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor

class MixinTargetInspection : MixinInspection() {

    override fun getStaticDescription() =
        "A Mixin class must target either one or more classes or provide one or more string targets"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            val mixin = psiClass.modifierList?.findAnnotation(MIXIN) ?: return

            // Check if @Mixin annotation has any targets defined
            if (
                mixin.findDeclaredAttributeValue("value") == null &&
                mixin.findDeclaredAttributeValue("targets") == null
            ) {
                holder.registerProblem(mixin, "@Mixin is missing a valid target class")
            }
        }
    }
}
