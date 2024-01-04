/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
