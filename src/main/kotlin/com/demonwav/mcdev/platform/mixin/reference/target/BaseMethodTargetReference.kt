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
import com.demonwav.mcdev.util.mapToArray
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult

internal abstract class BaseMethodTargetReference(element: PsiLiteral, methodReference: MixinReference)
    : TargetReference(element, methodReference) {

    protected abstract fun createFindUsagesVisitor(): FindUsagesVisitor
    protected abstract fun createCollectMethodsVisitor(): CollectMethodsVisitor

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val codeBlock = targetMethod?.body ?: return ResolveResult.EMPTY_ARRAY

        val visitor = createFindUsagesVisitor()
        codeBlock.accept(visitor)

        return visitor.usages.mapToArray(::PsiElementResolveResult)
    }

    override fun getVariants(): Array<out Any> {
        // TODO: Right now this will only work for Mixins with a single target class
        val target = this.targetMethod ?: return LookupElementBuilder.EMPTY_ARRAY
        val codeBlock = target.body ?: return LookupElementBuilder.EMPTY_ARRAY

        // Collect all method calls
        val visitor = createCollectMethodsVisitor()
        codeBlock.accept(visitor)

        val targetClass = target.containingClass!!

        return visitor.methods
                .mapToArray { (m, qualifier) ->
                    qualifyLookup(JavaLookupElementBuilder.forMethod(m, m.getQualifiedInternalNameAndDescriptor(qualifier),
                            PsiSubstitutor.EMPTY, targetClass)
                            .withLookupString(m.name), // Allow looking up targets by their method name
                            targetClass, m)
                }
    }
}

internal abstract class CollectMethodsVisitor : JavaRecursiveElementWalkingVisitor() {
    val methods = ArrayList<QualifiedMember<PsiMethod>>()
}

internal abstract class FindUsagesVisitor : JavaRecursiveElementWalkingVisitor() {
    val usages = ArrayList<PsiExpression>()
}
