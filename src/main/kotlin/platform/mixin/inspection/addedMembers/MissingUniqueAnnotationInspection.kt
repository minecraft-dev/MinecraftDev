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

package com.demonwav.mcdev.platform.mixin.inspection.addedMembers

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class MissingUniqueAnnotationInspection : AbstractAddedMembersInspection() {
    override fun getStaticDescription() = "Reports missing @Unique annotations"

    override fun visitAddedField(holder: ProblemsHolder, field: PsiField) {
        if (!field.hasAnnotation(MixinConstants.Annotations.UNIQUE)) {
            holder.registerProblem(
                field.nameIdentifier,
                "Missing @Unique annotation",
                AddAnnotationFix(MixinConstants.Annotations.UNIQUE, field)
            )
        }
    }

    override fun visitAddedMethod(holder: ProblemsHolder, method: PsiMethod, isInherited: Boolean) {
        if (!isInherited && !method.hasAnnotation(MixinConstants.Annotations.UNIQUE)) {
            holder.registerProblem(
                method.nameIdentifier ?: return,
                "Missing @Unique annotation",
                AddAnnotationFix(MixinConstants.Annotations.UNIQUE, method)
            )
        }
    }
}
