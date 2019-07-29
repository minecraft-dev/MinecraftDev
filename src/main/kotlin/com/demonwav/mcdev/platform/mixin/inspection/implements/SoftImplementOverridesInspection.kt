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

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.isSoftImplementMissingParent
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod

class SoftImplementOverridesInspection : MixinInspection() {

    override fun getStaticDescription() =
        "Reports soft-implemented methods in Mixins that do not override a method in the target classes."

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            if (method.isSoftImplementMissingParent()) {
                holder.registerProblem(
                    method.nameIdentifier ?: method,
                    "Method does not soft-implement a method from its interfaces"
                )
            }
        }
    }
}
