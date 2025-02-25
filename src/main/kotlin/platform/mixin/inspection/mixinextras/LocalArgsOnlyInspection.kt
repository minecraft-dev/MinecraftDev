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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifyVariableArgsOnlyInspection
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.MixinExtras.unwrapLocalRef
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.parentOfType

class LocalArgsOnlyInspection : MixinInspection() {
    override fun getStaticDescription() =
        "Checks that @Local has argsOnly if it targets arguments, which improves performance of the mixin"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitAnnotation(localAnnotation: PsiAnnotation) {
            if (localAnnotation.qualifiedName != MixinConstants.MixinExtras.LOCAL) {
                return
            }
            if (localAnnotation.findDeclaredAttributeValue("argsOnly")?.constantValue == true) {
                return
            }
            val parameter = localAnnotation.parentOfType<PsiParameter>() ?: return
            val method = parameter.findContainingMethod() ?: return

            val targets = method.annotations.mapFirstNotNull { annotation ->
                MixinAnnotationHandler.resolveTarget(annotation).asSequence()
                    .filterIsInstance<MethodTargetMember>()
                    .map { it.classAndMethod }
            } ?: return

            val localType = parameter.type.unwrapLocalRef()

            if (ModifyVariableArgsOnlyInspection.shouldReport(localAnnotation, localType, targets)) {
                holder.registerProblem(
                    localAnnotation.nameReferenceElement ?: localAnnotation,
                    "@Local may be argsOnly = true",
                    AnnotationAttributeFix(localAnnotation, "argsOnly" to true)
                )
            }
        }
    }
}
