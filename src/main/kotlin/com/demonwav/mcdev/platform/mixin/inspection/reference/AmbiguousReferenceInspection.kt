/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.METHOD_INJECTORS
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class AmbiguousReferenceInspection : MixinAnnotationAttributeInspection(METHOD_INJECTORS, "method") {

    override fun getStaticDescription() = "Reports ambiguous references in Mixin annotations"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val ambiguousReference = MethodReference.getReferenceIfAmbiguous(value) ?: return

        // TODO: Quick fix
        holder.registerProblem(value, "Ambiguous reference to method '${ambiguousReference.name}' in target class")
    }
}
