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
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression

object MethodTargetReference : TargetReference.MethodHandler() {

    override fun createFindUsagesVisitor(
        context: PsiElement,
        targetClass: PsiClass,
        checkOnly: Boolean
    ): CollectVisitor<out PsiElement>? {
        return MixinMemberReference.parse(context.constantStringValue)
            ?.let { FindUsagesVisitor(targetClass, it, checkOnly) }
    }

    override fun createCollectUsagesVisitor(): CollectVisitor<QualifiedMember<PsiMethod>> = CollectUsagesVisitor()

    private class FindUsagesVisitor(
        private val targetClass: PsiClass,
        private val target: MemberReference,
        checkOnly: Boolean
    ) :
        CompiledMethodsVisitor<PsiElement>(checkOnly) {

        override fun visitMethodUsage(method: PsiMethod, qualifier: PsiClass?, source: PsiElement) {
            if (target.match(method, qualifier ?: targetClass)) {
                addResult(source)
            }
        }
    }

    private class CollectUsagesVisitor : CompiledMethodsVisitor<QualifiedMember<PsiMethod>>(false) {

        override fun visitMethodUsage(method: PsiMethod, qualifier: PsiClass?, source: PsiElement) {
            addResult(QualifiedMember(method, qualifier))
        }
    }

    private abstract class CompiledMethodsVisitor<T>(checkOnly: Boolean) : CollectVisitor<T>(checkOnly) {

        protected abstract fun visitMethodUsage(method: PsiMethod, qualifier: PsiClass?, source: PsiElement)

        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
            val method = expression.resolveMethod()
            if (method != null) {
                val containingClass = method.containingClass

                // Normally, Java uses the type of the instance to qualify the method calls
                // However, if the method is part of java.lang.Object (e.g. equals or toString)
                // and no class in the hierarchy of the instance overrides the method, Java will
                // insert the call using java.lang.Object as the owner
                val qualifier =
                    if (method.isConstructor || containingClass?.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT)
                        containingClass else QualifiedMember.resolveQualifier(expression.methodExpression)

                visitMethodUsage(method, qualifier, expression)
            }

            super.visitMethodCallExpression(expression)
        }

        override fun visitNewExpression(expression: PsiNewExpression) {
            val constructor = expression.resolveConstructor()
            if (constructor != null) {
                visitMethodUsage(constructor, constructor.containingClass!!, expression)
            }

            super.visitNewExpression(expression)
        }

        override fun visitForeachStatement(statement: PsiForeachStatement) {
            // Enhanced for loops get compiled to a loop calling next on an Iterator
            // Since these method calls are not available in the method source we need
            // to generate 3 virtual method calls: the call to get the Iterator
            // (Iterable.iterator()) and "Iterator.next()" and "Iterator.hasNext()"

            val type = (statement.iteratedValue?.type as? PsiClassType)?.resolve()
            if (type != null) {
                // Find iterator() method
                val method = type.findMethodsByName("iterator", true).first { it.parameterList.parametersCount == 0 }
                if (method != null) {
                    visitMethodUsage(method, type, statement)
                }
            }

            // Get Iterator class to resolve next and hasNext
            val iteratorClass = JavaPsiFacade.getInstance(statement.project)
                .findClass(CommonClassNames.JAVA_UTIL_ITERATOR, statement.resolveScope)

            if (iteratorClass != null) {
                val hasNext =
                    iteratorClass.findMethodsByName("hasNext", false).first { it.parameterList.parametersCount == 0 }
                if (hasNext != null) {
                    visitMethodUsage(hasNext, iteratorClass, statement)
                }

                val next =
                    iteratorClass.findMethodsByName("next", false).first { it.parameterList.parametersCount == 0 }
                if (next != null) {
                    visitMethodUsage(next, iteratorClass, statement)
                }
            }

            super.visitForeachStatement(statement)
        }
    }
}
