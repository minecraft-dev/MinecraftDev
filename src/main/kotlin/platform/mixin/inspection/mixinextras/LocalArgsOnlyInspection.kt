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
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifyVariableArgsOnlyInspection
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.unwrapLocalRef
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.parentOfType

class LocalArgsOnlyInspection : MixinInspection() {
    override fun getStaticDescription() =
        "Checks that @Local has argsOnly if it targets arguments, which improves performance of the mixin"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitAnnotation(localAnnotation: PsiAnnotation) {
            if (!localAnnotation.hasQualifiedName(MixinConstants.MixinExtras.LOCAL)) {
                return
            }
            if (localAnnotation.findDeclaredAttributeValue("argsOnly")?.constantValue == true) {
                return
            }
            val parameter = localAnnotation.parentOfType<PsiParameter>() ?: return
            val method = parameter.declarationScope as? PsiMethod ?: return

            val (injector, injectorAnnotation) = method.annotations.mapFirstNotNull { annotation ->
                (MixinAnnotationHandler.forMixinAnnotation(annotation, holder.project) as? InsnInjectorAnnotationHandler)?.let { it to annotation }
            } ?: return

            val localType = parameter.type.unwrapLocalRef()
            val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)

            if (ModifyVariableArgsOnlyInspection.Util.shouldReport(localInfo, injector, injectorAnnotation)) {
                holder.registerProblem(
                    localAnnotation.nameReferenceElement ?: localAnnotation,
                    "@Local may be argsOnly = true",
                    AnnotationAttributeFix(localAnnotation, "argsOnly" to true)
                )
            }
        }
    }
}
