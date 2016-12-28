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
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression

internal class MethodTargetReference(element: PsiElement, methodReference: MixinReference)
    : BaseMethodTargetReference(element, methodReference) {

    override val description: String
        get() = "method '$value' in target method"

    override fun createFindUsagesVisitor(): CollectVisitor<PsiExpression> = FindMethodUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectVisitor<QualifiedMember<PsiMethod>> = CollectCalledMethodsVisitor()

}

private abstract class CollectMethodsVisitor<T>  : CollectVisitor<T>() {

    protected abstract fun visitCallExpression(expression: PsiCallExpression, qualifier: PsiClassType?)

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        visitCallExpression(expression, findQualifierType(expression.methodExpression))
        super.visitMethodCallExpression(expression)
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        visitCallExpression(expression, null)
        super.visitNewExpression(expression)
    }

}

private class FindMethodUsagesVisitor(val qinad: String) : CollectMethodsVisitor<PsiExpression>() {

    override fun visitCallExpression(expression: PsiCallExpression, qualifier: PsiClassType?) {
        val method = expression.resolveMethod()
        if (method != null && method.getQualifiedInternalNameAndDescriptor(qualifier) == this.qinad) {
            result.add(expression)
        }
    }

}

private class CollectCalledMethodsVisitor : CollectMethodsVisitor<QualifiedMember<PsiMethod>>() {

    override fun visitCallExpression(expression: PsiCallExpression, qualifier: PsiClassType?) {
        val method = expression.resolveMethod()
        if (method != null) {
            result.add(QualifiedMember(method, qualifier))
        }
    }

}
