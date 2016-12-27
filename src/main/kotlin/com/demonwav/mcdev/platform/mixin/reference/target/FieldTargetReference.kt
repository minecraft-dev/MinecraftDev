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
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.qualifiedNameAndDescriptor
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.ResolveResult

internal class FieldTargetReference(element: PsiLiteral, methodReference: MixinReference) : TargetReference(element, methodReference) {

    override val description: String
        get() = "field target '$value' in target method"

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val codeBlock = targetMethod?.body ?: return ResolveResult.EMPTY_ARRAY

        val visitor = FindFieldUsagesVisitor(value)
        codeBlock.accept(visitor)
        return visitor.usages.mapToArray(::PsiElementResolveResult)
    }

    override fun getVariants(): Array<out Any> {
        val target = this.targetMethod ?: return LookupElementBuilder.EMPTY_ARRAY
        val codeBlock = target.body ?: return LookupElementBuilder.EMPTY_ARRAY

        // Collect all field references
        val visitor = CollectReferencedFieldsVisitor()
        codeBlock.accept(visitor)

        val targetClass = target.containingClass!!

        return visitor.fields
                .mapToArray { f ->
                    qualifyLookup(JavaLookupElementBuilder.forField(f, f.qualifiedNameAndDescriptor, targetClass)
                            .withPresentableText(f.name!!)
                            .withLookupString(f.name!!), targetClass, f)
                }
    }

}

private class CollectReferencedFieldsVisitor : JavaRecursiveElementWalkingVisitor() {

    val fields = ArrayList<PsiField>()

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        if (expression !is PsiMethodReferenceExpression) {
            val resolved = expression.resolve()
            if (resolved is PsiField) {
                fields.add(resolved)
            }
        }

        super.visitReferenceExpression(expression)
    }

}

private class FindFieldUsagesVisitor(val qnad: String) : JavaRecursiveElementWalkingVisitor() {

    val usages = ArrayList<PsiReferenceExpression>()

    override fun visitReferenceExpression(expression: PsiReferenceExpression) {
        if (expression !is PsiMethodReferenceExpression) {
            // TODO: Optimize this so we don't need to resolve all fields to find a reference
            val resolved = expression.resolve()
            if (resolved is PsiField && resolved.qualifiedNameAndDescriptor == this.qnad) {
                usages.add(expression)
            }
        }

        super.visitReferenceExpression(expression)
    }

}
