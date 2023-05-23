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

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class UnqualifiedMemberReferenceInspection : MixinAnnotationAttributeInspection(AT, "target") {

    override fun getStaticDescription() = "Reports unqualified member references in @At targets"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder,
    ) {
        // Check if the specified target reference uses member descriptors
        if (!AtResolver.usesMemberReference(annotation)) {
            return
        }

        // TODO: Quick fix

        val selector = parseMixinSelector(value) ?: return
        if (!selector.qualified) {
            holder.registerProblem(value, "Unqualified member reference in @At target")
            return
        }

        if (selector.methodDescriptor == null && selector.fieldDescriptor == null) {
            holder.registerProblem(value, "Method/field descriptor is required for member reference in @At target")
        }
    }
}
