/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiConstructorCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNewExpression

class MixinInstantiationWithinMixinInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when a mixin class is instantiated within that same class using a constructor which doesn't exist in the target class"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitNewExpression(expression: PsiNewExpression) {
            if (expression.isArrayCreation || expression.anonymousClass != null) {
                return
            }
            val classReference = expression.classReference ?: return
            val constructedClass = classReference.resolve() as? PsiClass ?: return
            visitConstructorCall(expression, constructedClass, classReference)
        }

        override fun visitEnumConstant(enumConstant: PsiEnumConstant) {
            val constructedClass = enumConstant.containingClass ?: return
            visitConstructorCall(enumConstant, constructedClass, enumConstant.nameIdentifier)
        }

        private fun visitConstructorCall(constructorCall: PsiConstructorCall, constructedClass: PsiClass, problemElement: PsiElement) {
            if (!constructedClass.isMixin || constructedClass != constructorCall.findContainingClass()) {
                return
            }
            val resolvedConstructorDesc = constructorCall.resolveConstructor()?.descriptor ?: when {
                constructedClass.isEnum -> "(Ljava/lang/String;)V"
                !constructedClass.hasModifierProperty(PsiModifier.STATIC) && constructedClass.containingClass != null ->
                    "(${constructedClass.containingClass!!.descriptor})V"
                else -> "()V"
            }

            val constructorExistsInAllTargets = constructedClass.mixinTargets.all {
                it.findMethod(
                    MemberReference(
                        "<init>",
                        resolvedConstructorDesc
                    )
                ) != null
            }
            if (constructorExistsInAllTargets) {
                return
            }

            holder.registerProblem(problemElement, "Constructor does not exist in target class")
        }
    }
}
