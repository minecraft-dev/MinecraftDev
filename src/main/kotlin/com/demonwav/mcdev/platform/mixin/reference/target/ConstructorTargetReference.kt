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

import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNewExpression

object ConstructorTargetReference : TargetReference.Handler<PsiClass>() {

    override fun resolveTarget(context: PsiElement): PsiElement? {
        val name = context.constantStringValue?.replace('/', '.') ?: return null
        return findQualifiedClass(name, context)
    }

    override fun createFindUsagesVisitor(
        context: PsiElement,
        targetClass: PsiClass,
        checkOnly: Boolean
    ): CollectVisitor<out PsiElement>? {
        val name = context.constantStringValue?.replace('/', '.') ?: return null
        return FindUsagesVisitor(name, checkOnly)
    }

    override fun createCollectUsagesVisitor(): CollectVisitor<PsiClass> = CollectUsagesVisitor()

    override fun createLookup(targetClass: PsiClass, element: PsiClass): LookupElementBuilder? {
        return JavaLookupElementBuilder.forClass(element, element.internalName)
            .withPresentableText(element.shortName ?: return null)
    }

    private fun resolveConstructedClass(expression: PsiNewExpression): PsiClass? {
        return expression.anonymousClass ?: expression.classReference?.resolve() as PsiClass
    }

    private class FindUsagesVisitor(private val qualifiedName: String, checkOnly: Boolean) :
        CollectVisitor<PsiNewExpression>(checkOnly) {

        override fun visitNewExpression(expression: PsiNewExpression) {
            val psiClass = resolveConstructedClass(expression)
            if (psiClass != null && psiClass.qualifiedName == this.qualifiedName) {
                addResult(expression)
            }

            super.visitNewExpression(expression)
        }
    }

    private class CollectUsagesVisitor : CollectVisitor<PsiClass>(false) {

        override fun visitNewExpression(expression: PsiNewExpression) {
            val psiClass = resolveConstructedClass(expression)
            if (psiClass != null) {
                addResult(psiClass)
            }

            super.visitNewExpression(expression)
        }
    }
}
