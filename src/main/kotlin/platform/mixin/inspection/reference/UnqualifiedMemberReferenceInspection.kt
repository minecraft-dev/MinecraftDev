/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
        holder: ProblemsHolder
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
