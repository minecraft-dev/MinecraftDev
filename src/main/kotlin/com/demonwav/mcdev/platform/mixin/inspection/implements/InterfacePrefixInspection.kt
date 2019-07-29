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
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue

class InterfacePrefixInspection : MixinAnnotationAttributeInspection(INTERFACE, "prefix") {

    override fun getStaticDescription() = "Reports invalid prefixes in @Interface annotations. " +
        "The prefixes must end with a dollar sign ($)."

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val prefix = value.constantStringValue ?: return
        if (!prefix.endsWith('$')) {
            holder.registerProblem(value, "@Interface prefix must end with a dollar sign ($)")
        }
    }
}
