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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.isAssignable
import com.demonwav.mcdev.util.McdevDfaUtil
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil

class WrongOperationParametersInspection : MixinInspection() {
    override fun getStaticDescription() = "Operation.call called with the wrong parameter types"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (expression.methodExpression.referenceName != "call") {
                return
            }
            if (expression.resolveMethod()?.containingClass?.qualifiedName != MixinConstants.MixinExtras.OPERATION) {
                return
            }

            val containingMethod = PsiTreeUtil.getParentOfType(
                expression,
                PsiMethod::class.java,
                true,
                PsiClass::class.java,
                PsiField::class.java
            ) ?: return

            if (!containingMethod.hasAnnotation(MixinConstants.MixinExtras.WRAP_OPERATION)) {
                return
            }

            val (operationIndex, operationParam) = containingMethod.parameterList.parameters.asSequence()
                .withIndex()
                .firstOrNull { (_, param) ->
                    (param.type as? PsiClassType)?.resolve()?.qualifiedName == MixinConstants.MixinExtras.OPERATION
                } ?: return
            val expectedParamTypes = containingMethod.parameterList.parameters.asSequence()
                .take(operationIndex)
                .map { it.type }
                .toList()

            val qualifier = expression.methodExpression.qualifierExpression ?: return
            if (!McdevDfaUtil.isSometimesEqualToParameter(qualifier, operationParam)) {
                return
            }

            if (expression.argumentList.expressionCount == expectedParamTypes.size) {
                val allValid = expression.argumentList.expressions.zip(expectedParamTypes).all { (expr, expectedType) ->
                    val exprType = McdevDfaUtil.getDataflowType(expr) ?: return@all true
                    isAssignable(expectedType, exprType, false)
                }
                if (allValid) {
                    return
                }
            }

            val problemElement = expression.argumentList
            holder.registerProblem(problemElement, "Operation.call called with the wrong parameter types")
        }
    }
}
