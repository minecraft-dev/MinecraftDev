/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.isAccessorMixin
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiTypeElement

class MixinClassReferenceInspection : MixinInspection() {

    override fun getStaticDescription() =
        "A Mixin class doesn't exist at runtime, and thus cannot be referenced directly."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitTypeElement(type: PsiTypeElement) {
            val classType = type.type as? PsiClassType ?: return
            val referencedClass = classType.resolve() ?: return

            if (!referencedClass.isMixin) {
                return
            }

            // Check if the the reference is a super Mixin
            val psiClass = type.findContainingClass() ?: return
            if (psiClass.isEquivalentTo(referencedClass) || psiClass.isInheritor(referencedClass, true)) {
                // Mixin class is part of the hierarchy
                return
            }

            // Check if the reference is an accessor Mixin
            if (referencedClass.isAccessorMixin) {
                return
            }

            holder.registerProblem(type, "Mixin class cannot be referenced directly")
        }
    }
}
