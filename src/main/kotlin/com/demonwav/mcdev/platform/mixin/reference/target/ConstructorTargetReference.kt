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
import com.demonwav.mcdev.util.internalName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNewExpression

internal class ConstructorTargetReference(element: PsiElement, methodReference: MixinReference)
    : TargetReference<PsiClass>(element, methodReference) {

    override val description: String
        get() = "constructor '$value' in target method"

    override fun createLookup(targetClass: PsiClass, element: PsiClass): LookupElementBuilder {
        val name = element.internalName
        return JavaLookupElementBuilder.forClass(element, name)
                .withPresentableText(name.substring(name.lastIndexOf('/') + 1))
    }

    override fun createFindUsagesVisitor(): CollectVisitor<PsiNewExpression> = FindConstructorUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectVisitor<PsiClass> = CollectCalledConstructorsVisitor()

}

private fun resolveConstructedClass(expression: PsiNewExpression): PsiClass? {
    return expression.anonymousClass ?: expression.classReference?.resolve() as PsiClass
}

private class FindConstructorUsagesVisitor(val internalName: String) : CollectVisitor<PsiNewExpression>() {

    override fun visitNewExpression(expression: PsiNewExpression) {
        val psiClass = resolveConstructedClass(expression)
        if (psiClass != null && psiClass.internalName == this.internalName) {
            result.add(expression)
        }

        super.visitNewExpression(expression)
    }

}

private class CollectCalledConstructorsVisitor : CollectVisitor<PsiClass>() {

    override fun visitNewExpression(expression: PsiNewExpression) {
        val psiClass = resolveConstructedClass(expression)
        if (psiClass != null) {
            result.add(psiClass)
        }

        super.visitNewExpression(expression)
    }

}
