/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection.addedMembers

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.SuperMethodsSearch

class MissingUniqueAnnotationInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports missing @Unique annotations"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {
        override fun visitField(field: PsiField) {
            if (field.containingClass?.isMixin != true) {
                return
            }

            if (field.hasAnnotation(MixinConstants.Annotations.SHADOW)) {
                return
            }

            if (!field.hasAnnotation(MixinConstants.Annotations.UNIQUE)) {
                holder.registerProblem(
                    field.nameIdentifier,
                    "Missing @Unique annotation",
                    AddAnnotationFix(MixinConstants.Annotations.UNIQUE, field)
                )
            }
        }

        override fun visitMethod(method: PsiMethod) {
            if (method.containingClass?.isMixin != true) {
                return
            }

            if (method.isConstructor) {
                return
            }

            val superMethod = SuperMethodsSearch.search(method, null, true, false).findFirst()
            if (superMethod != null) {
                return
            }

            val hasMixinAnnotation = method.annotations.any {
                val fqn = it.qualifiedName ?: return@any false
                fqn in ignoredMethodAnnotations || MixinAnnotationHandler.forMixinAnnotation(
                    fqn,
                    holder.project
                ) != null
            }
            if (hasMixinAnnotation) {
                return
            }

            holder.registerProblem(
                method.nameIdentifier ?: return,
                "Missing @Unique annotation",
                AddAnnotationFix(MixinConstants.Annotations.UNIQUE, method)
            )
        }
    }

    companion object {
        private val ignoredMethodAnnotations = setOf(
            MixinConstants.Annotations.UNIQUE,
            MixinConstants.Annotations.SHADOW,
            MixinConstants.Annotations.ACCESSOR,
            MixinConstants.Annotations.INVOKER,
            MixinConstants.Annotations.OVERWRITE,
            MixinConstants.Annotations.INTRINSIC,
            MixinConstants.Annotations.SOFT_OVERRIDE,
        )
    }
}
