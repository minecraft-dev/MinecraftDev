/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.implements

import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INTERFACE
import com.demonwav.mcdev.util.resolveClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class InterfaceIsInterfaceInspection : MixinAnnotationAttributeInspection(INTERFACE, "iface") {

    override fun getStaticDescription() = "Reports usages of @Interface with a regular class instead of an interface."

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val psiClass = value.resolveClass() ?: return
        if (!psiClass.isInterface) {
            holder.registerProblem(value, "Interface expected here")
        }
    }
}
