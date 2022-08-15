/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.parentOfType

class MixinInnerClassInspection : MixinInspection() {

    override fun getStaticDescription() = "Reports invalid usages of inner classes in Mixins."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            val outerClass = psiClass.containingClass ?: return
            if (!outerClass.isMixin) {
                if (outerClass is PsiAnonymousClass && outerClass.parentOfType<PsiClass>()?.isMixin == true) {
                    holder.registerProblem(
                        psiClass,
                        "Inner class not allowed inside anonymous classes inside mixins"
                    )
                }

                return
            }

            // Static inner classes are fine if they are also Mixins
            if (psiClass.isMixin) {
                // Ensure inner class is static
                if (!psiClass.hasModifierProperty(PsiModifier.STATIC)) {
                    holder.registerProblem(
                        psiClass.modifierList!!,
                        "@Mixin inner class must be static",
                        QuickFixFactory.getInstance().createModifierListFix(psiClass, PsiModifier.STATIC, true, false)
                    )
                }
            } else {
                holder.registerProblem(psiClass, "Inner classes are only allowed if they are also @Mixin classes")
            }
        }

        override fun visitAnonymousClass(psiClass: PsiAnonymousClass) {
            val outerClass = psiClass.parent?.findContainingClass() ?: return
            if (outerClass !is PsiAnonymousClass) {
                return
            }
            if (outerClass.parentOfType<PsiClass>()?.isMixin != true) {
                return
            }

            holder.registerProblem(psiClass, "Double nested anonymous classes are not allowed in a @Mixin class")
        }
    }
}
