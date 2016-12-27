/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.util.qualifiedInternalNameAndDescriptor
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable

internal class ConstantStringMethodTargetReference(element: PsiLiteral, methodReference: MixinReference)
    : BaseMethodTargetReference(element, methodReference) {

    override val description: String
        get() = "method target '$value' in target method"

    override fun createFindUsagesVisitor(): FindUsagesVisitor = FindConstantStringMethodUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectMethodsVisitor = CollectConstantStringMethodUsagesVisitor()

}

private fun isConstantStringMethodCall(expression: PsiMethodCallExpression): Boolean {
    // Must return void
    if (expression.type != PsiType.VOID) {
        return false
    }

    val arguments = expression.argumentList
    val argumentTypes = arguments.expressionTypes
    if (argumentTypes.size != 1 || argumentTypes[0] != PsiType.getJavaLangString(expression.manager, expression.resolveScope)) {
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

private class FindConstantStringMethodUsagesVisitor(val qinad: String): FindUsagesVisitor() {

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        if (isConstantStringMethodCall(expression)) {
            val method = expression.resolveMethod()
            if (method != null && method.qualifiedInternalNameAndDescriptor == this.qinad) {
                usages.add(expression)
            }
        }
    }

}

private class CollectConstantStringMethodUsagesVisitor : CollectMethodsVisitor() {

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        if (isConstantStringMethodCall(expression)) {
            val method = expression.resolveMethod()
            if (method != null) {
                methods.add(method)
            }
        }

        super.visitMethodCallExpression(expression)
    }

}
