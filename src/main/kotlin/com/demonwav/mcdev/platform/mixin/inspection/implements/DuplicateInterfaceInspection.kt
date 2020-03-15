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
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.resolveClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass

class DuplicateInterfaceInspection : MixinAnnotationAttributeInspection(MixinConstants.Annotations.IMPLEMENTS, null) {

    override fun getStaticDescription() = "Reports duplicate @Interface classes in an @Implements annotation."

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val interfaces = value.findAnnotations().ifEmpty { return }

        val classes = ArrayList<PsiClass>()
        for (iface in interfaces) {
            // TODO: Can we check this without resolving the class?
            val psiClass = iface.findDeclaredAttributeValue("iface")?.resolveClass() ?: continue

            if (classes.any { it equivalentTo psiClass }) {
                holder.registerProblem(iface, "Interface is already implemented", RemoveAnnotationQuickFix(iface, null))
            } else {
                classes.add(psiClass)
            }
        }
    }
}
