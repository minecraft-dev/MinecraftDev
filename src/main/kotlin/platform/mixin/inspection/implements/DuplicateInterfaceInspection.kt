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

package com.demonwav.mcdev.platform.mixin.inspection.implements

import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.resolveClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass

class DuplicateInterfaceInspection : MixinAnnotationAttributeInspection(MixinConstants.Annotations.IMPLEMENTS, null) {

    override fun getStaticDescription() = "Reports duplicate @Interface classes in an @Implements annotation."

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder,
    ) {
        val interfaces = value.findAnnotations().ifEmpty { return }

        val classes = ArrayList<PsiClass>()
        for (iface in interfaces) {
            // TODO: Can we check this without resolving the class?
            val psiClass = iface.findDeclaredAttributeValue("iface")?.resolveClass() ?: continue

            if (classes.any { it equivalentTo psiClass }) {
                holder.registerProblem(iface, "Interface is already implemented", RemoveAnnotationQuickFix(iface, null))
            } else {
                classes.add(psiClass)
            }
        }
    }
}
