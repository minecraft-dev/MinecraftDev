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

import com.demonwav.mcdev.platform.mixin.handlers.InsnInjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifyVariableNameWithoutLVTInspection
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.unwrapLocalRef
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.parentOfType

class LocalNameWithoutLVTInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitAnnotation(localAnnotation: PsiAnnotation) {
            if (!localAnnotation.hasQualifiedName(MixinConstants.MixinExtras.LOCAL)) {
                return
            }

            val parameter = localAnnotation.parentOfType<PsiParameter>() ?: return
            val method = parameter.declarationScope as? PsiMethod ?: return

            val localType = parameter.type.unwrapLocalRef()
            val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)

            val injectorAnnotation = method.annotations.firstOrNull { annotation ->
                MixinAnnotationHandler.forMixinAnnotation(annotation, holder.project) is InsnInjectorAnnotationHandler
            } ?: return

            val problemElement = ModifyVariableNameWithoutLVTInspection.Util.getProblemElement(
                localInfo,
                localAnnotation,
                injectorAnnotation,
            ) ?: return
            holder.registerProblem(
                problemElement,
                "@Local uses a variable name, but the target class has no local variable names at runtime",
            )
        }
    }
}
