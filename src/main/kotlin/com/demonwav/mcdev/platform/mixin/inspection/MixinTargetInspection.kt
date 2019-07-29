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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor

class MixinTargetInspection : MixinInspection() {

    override fun getStaticDescription() =
        "A Mixin class must target either one or more classes or provide one or more string targets"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            val mixin = psiClass.modifierList?.findAnnotation(MIXIN) ?: return

            // Check if @Mixin annotation has any targets defined
            if (
                mixin.findDeclaredAttributeValue("value") == null &&
                mixin.findDeclaredAttributeValue("targets") == null
            ) {
                holder.registerProblem(mixin, "@Mixin is missing a valid target class")
            }
        }
    }
}
