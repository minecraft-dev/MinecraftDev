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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.InsnInjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor

class DiscouragedShiftInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports discouraged usages of shifting in injection points"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                return
            }
            val injectorAnnotation = AtResolver.findInjectorAnnotation(annotation) ?: return
            val injector = MixinAnnotationHandler.forMixinAnnotation(injectorAnnotation, holder.project) as? InsnInjectorAnnotationHandler
                ?: return
            val atValue = annotation.findDeclaredAttributeValue("value") ?: return
            val atCode = atValue.constantStringValue ?: return
            val shift = AtResolver.getShift(annotation)
            if (isShiftDiscouraged(shift, annotation, injector, atCode)) {
                val shiftElement = annotation.findDeclaredAttributeValue("shift") ?: return
                holder.registerProblem(shiftElement, "Shifting like this is discouraged because it's brittle")
            }
        }
    }

    private fun isShiftDiscouraged(shift: Int, at: PsiAnnotation, injector: InsnInjectorAnnotationHandler, atCode: String): Boolean {
        if (injector.isShiftAlwaysDiscouraged) {
            return shift != 0
        }

        return InjectionPoint.byAtCode(atCode)?.isShiftDiscouraged(shift, at) == true
    }
}
