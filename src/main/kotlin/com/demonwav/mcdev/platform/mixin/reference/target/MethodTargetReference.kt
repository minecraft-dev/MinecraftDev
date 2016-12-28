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
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression

internal class MethodTargetReference(element: PsiElement, methodReference: MixinReference)
    : BaseMethodTargetReference(element, methodReference) {

    override val description: String
        get() = "method '$value' in target method"

    override fun createFindUsagesVisitor(): CollectVisitor<PsiElement> = FindMethodUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectVisitor<QualifiedMember<PsiMethod>> = CollectCalledMethodsVisitor()

}

private abstract class CollectMethodsVisitor<T>  : CollectVisitor<T>() {

    protected abstract fun visitMethodUsage(method: PsiMethod, qualifier: PsiClassType?, source: PsiElement)

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        val method = expression.resolveMethod()
        if (method != null) {
            visitMethodUsage(method, findQualifierType(expression.methodExpression), expression)
        }

        super.visitMethodCallExpression(expression)
    }

    override fun visitNewExpression(expression: PsiNewExpression) {
        val constructor = expression.resolveConstructor()
        if (constructor != null) {
            visitMethodUsage(constructor, null, expression)
        }

        super.visitNewExpression(expression)
    }

    override fun visitForeachStatement(statement: PsiForeachStatement) {
        // Enhanced for loops get compiled to a loop calling next on an Iterator
        // Since these method calls are not available in the method source we need
        // to generate 3 virtual method calls: the call to get the Iterator
        // (Iterable.iterator()) and "Iterator.next()" and "Iterator.hasNext()"

        val type = statement.iteratedValue?.type as? PsiClassType
        if (type != null) {
            // Find iterator() method
            val method = type.resolve()?.findMethodsByName("iterator", true)?.first { it.parameterList.parametersCount == 0 }
            if (method != null) {
                visitMethodUsage(method, type, statement)
            }
        }

        // Get Iterator class to resolve next and hasNext
        val iteratorClass = JavaPsiFacade.getInstance(statement.project)
                .findClass(CommonClassNames.JAVA_UTIL_ITERATOR, statement.resolveScope)

        if (iteratorClass != null) {
            val hasNext = iteratorClass.findMethodsByName("hasNext", false).first { it.parameterList.parametersCount == 0 }
            if (hasNext != null) {
                visitMethodUsage(hasNext, null, statement)
            }

            val next = iteratorClass.findMethodsByName("next", false).first { it.parameterList.parametersCount == 0 }
            if (next != null) {
                visitMethodUsage(next, null, statement)
            }
        }

        super.visitForeachStatement(statement)
    }

}

private class FindMethodUsagesVisitor(val qinad: String) : CollectMethodsVisitor<PsiElement>() {

    override fun visitMethodUsage(method: PsiMethod, qualifier: PsiClassType?, source: PsiElement) {
        if (method.getQualifiedInternalNameAndDescriptor(qualifier) == this.qinad) {
            result.add(source)
        }
    }

}

private class CollectCalledMethodsVisitor : CollectMethodsVisitor<QualifiedMember<PsiMethod>>() {

    override fun visitMethodUsage(method: PsiMethod, qualifier: PsiClassType?, source: PsiElement) {
        result.add(QualifiedMember(method, qualifier))
    }

}
