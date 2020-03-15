/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.overwrite

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMethod

abstract class OverwriteInspection : MixinInspection() {

    protected abstract fun visitOverwrite(holder: ProblemsHolder, method: PsiMethod, overwrite: PsiAnnotation)

    final override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private inner class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitMethod(method: PsiMethod) {
            // Check if the method is an @Overwrite
            val overwrite = method.findAnnotation(MixinConstants.Annotations.OVERWRITE) ?: return
            visitOverwrite(holder, method, overwrite)
        }
    }
}
