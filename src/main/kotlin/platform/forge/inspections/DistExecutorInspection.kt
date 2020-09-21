/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor

class DistExecutorInspection : BaseInspection() {
    override fun getDisplayName() = "DistExecutor problems"

    override fun getStaticDescription() = "DistExecutor problems"

    override fun buildErrorString(vararg infos: Any?): String {
        return infos[0] as String
    }

    override fun buildVisitor(): BaseInspectionVisitor {
        return Visitor()
    }

    private class Visitor : BaseInspectionVisitor() {
        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            val method = expression.resolveMethod() ?: return
            if (method.containingClass?.qualifiedName != ForgeConstants.DIST_EXECUTOR) return
            when (method.name) {
                "safeCallWhenOn", "safeRunWhenOn" -> {
                    checkSafeArgument(method.name, expression.argumentList.expressions.getOrNull(1))
                }
                "safeRunForDist" -> {
                    for (arg in expression.argumentList.expressions) {
                        checkSafeArgument(method.name, arg)
                    }
                }
            }
        }

        private fun checkSafeArgument(methodName: String, expression: PsiExpression?) {
            if (expression == null || expression is PsiErrorElement || expression.textLength == 0) return

            if (expression !is PsiLambdaExpression) {
                registerError(expression, "DistExecutor.$methodName must contain lambda argument")
                return
            }

            val lambdaBody = expression.body
            if (lambdaBody != null &&
                lambdaBody !is PsiMethodReferenceExpression &&
                lambdaBody !is PsiErrorElement &&
                lambdaBody.textLength != 0
            ) {
                registerError(lambdaBody, "DistExecutor.$methodName must contain a method reference inside a lambda")
            }
        }
    }
}
