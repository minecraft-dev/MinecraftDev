/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantValue
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElementVisitor

class CtorHeadUsedForNonConstructorInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when CTOR_HEAD is used without targeting a constructor method"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = object : JavaElementVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                return
            }
            val atValue = annotation.findDeclaredAttributeValue("value") ?: return
            if (atValue.constantValue != "CTOR_HEAD") {
                return
            }
            if (!UnnecessaryUnsafeInspection.mightTargetConstructor(annotation)) {
                holder.registerProblem(
                    atValue,
                    "CTOR_HEAD used without targeting a constructor",
                    ReplaceWithHeadFix(annotation),
                )
            }
        }
    }

    private class ReplaceWithHeadFix(at: PsiAnnotation) :
        AnnotationAttributeFix(at, "value" to "HEAD", "unsafe" to null) {
        override fun getFamilyName() = "Replace with \"HEAD\""
        override fun getText() = "Replace with \"HEAD\""
    }
}
