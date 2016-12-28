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
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression

internal class FieldTargetReference(element: PsiElement, methodReference: MixinReference)
    : QualifiedTargetReference<PsiField>(element, methodReference) {

    override val description: String
        get() = "field '$value' in target method"

    override fun createFindUsagesVisitor(): CollectVisitor<PsiReferenceExpression> = FindFieldUsagesVisitor(value)
    override fun createCollectMethodsVisitor(): CollectVisitor<QualifiedMember<PsiField>> = CollectReferencedFieldsVisitor()

    override fun createLookup(targetClass: PsiClass, m: PsiField, qualifier: PsiClassType?): LookupElementBuilder {
        return JavaLookupElementBuilder.forField(m, m.getQualifiedInternalNameAndDescriptor(qualifier), targetClass)
                .withPresentableText(m.name!!)
                .withLookupString(m.name!!)
    }

}

private class FindFieldUsagesVisitor(val qnad: String) : CollectVisitor<PsiReferenceExpression>() {

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        if (expression !is PsiMethodReferenceExpression) {
            // TODO: Optimize this so we don't need to resolve all fields to find a reference
            val resolved = expression.resolve()
            if (resolved is PsiField && resolved.getQualifiedInternalNameAndDescriptor(findQualifierType(expression)) == this.qnad) {
                result.add(expression)
            }
        }

        super.visitReferenceExpression(expression)
    }

}

private class CollectReferencedFieldsVisitor : CollectVisitor<QualifiedMember<PsiField>>() {

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        if (expression !is PsiMethodReferenceExpression) {
            val resolved = expression.resolve()
            if (resolved is PsiField) {
                result.add(QualifiedMember(resolved, expression))
            }
        }

        super.visitReferenceExpression(expression)
    }

}
