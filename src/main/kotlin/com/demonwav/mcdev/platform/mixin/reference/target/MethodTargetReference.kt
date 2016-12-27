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
import com.demonwav.mcdev.util.getQualifiedInternalNameAndDescriptor
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethodCallExpression

internal class MethodTargetReference(element: PsiLiteral, methodReference: MixinReference)
    : BaseMethodTargetReference(element, methodReference) {

    override val description: String
        get() = "method target '$value' in target method"

    override fun createFindUsagesVisitor(): FindUsagesVisitor = FindMethodUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectMethodsVisitor = CollectCalledMethodsVisitor()

}

private class FindMethodUsagesVisitor(val qinad: String) : FindUsagesVisitor() {

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        // TODO: Optimize this so we don't need to resolve all methods to find a reference
        val method = expression.resolveMethod()
        if (method != null && method.getQualifiedInternalNameAndDescriptor(findQualifierType(expression.methodExpression)) == this.qinad) {
            usages.add(expression)
        }

        super.visitMethodCallExpression(expression)
    }

}

private class CollectCalledMethodsVisitor : CollectMethodsVisitor() {

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        val method = expression.resolveMethod()
        if (method != null) {
            methods.add(QualifiedMember(method, expression.methodExpression))
        }

        super.visitMethodCallExpression(expression)
    }

}
