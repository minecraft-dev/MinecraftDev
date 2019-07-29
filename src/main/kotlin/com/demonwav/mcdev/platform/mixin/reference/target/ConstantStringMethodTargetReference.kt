/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

object ConstantStringMethodTargetReference : TargetReference.MethodHandler() {

    override fun createFindUsagesVisitor(
        context: PsiElement,
        targetClass: PsiClass,
        checkOnly: Boolean
    ): CollectVisitor<out PsiElement>? {
        return MixinMemberReference.parse(context.constantStringValue)
            ?.let { FindUsagesVisitor(targetClass, it, checkOnly) }
    }

    override fun createCollectUsagesVisitor(): CollectVisitor<QualifiedMember<PsiMethod>> = CollectUsagesVisitor()

    private fun isConstantStringMethodCall(expression: PsiMethodCallExpression): Boolean {
        // Must return void
        if (expression.type != PsiType.VOID) {
            return false
        }

        val arguments = expression.argumentList
        val argumentTypes = arguments.expressionTypes
        if (argumentTypes.size != 1 || argumentTypes[0] != PsiType.getJavaLangString(
                expression.manager,
                expression.resolveScope
            )
        ) {
            // Must have one String parameter
            return false
        }

        val expr = arguments.expressions[0]
        // Expression must be constant, so either a literal or a constant field reference
        return when (expr) {
            is PsiLiteral -> true
            is PsiReference -> (expr.resolve() as? PsiVariable)?.computeConstantValue() != null
            else -> false
        }
    }

    private class FindUsagesVisitor(
        private val targetClass: PsiClass,
        private val target: MemberReference,
        checkOnly: Boolean
    ) :
        CollectVisitor<PsiExpression>(checkOnly) {

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (isConstantStringMethodCall(expression)) {
                val method = expression.resolveMethod()
                if (method != null && target.match(
                        method,
                        QualifiedMember.resolveQualifier(expression.methodExpression) ?: targetClass
                    )
                ) {
                    addResult(expression)
                }
            }

            super.visitMethodCallExpression(expression)
        }
    }

    private class CollectUsagesVisitor : CollectVisitor<QualifiedMember<PsiMethod>>(false) {

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            if (isConstantStringMethodCall(expression)) {
                val method = expression.resolveMethod()
                if (method != null) {
                    addResult(QualifiedMember(method, expression.methodExpression))
                }
            }

            super.visitMethodCallExpression(expression)
        }
    }
}
