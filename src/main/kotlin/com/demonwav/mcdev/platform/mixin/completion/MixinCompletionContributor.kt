/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.util.findShadowTargets
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.filter
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.JavaCompletionContributor
import com.intellij.codeInsight.completion.JavaCompletionSorting
import com.intellij.codeInsight.completion.LegacyCompletionContributor
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaReference
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiThisExpression

class MixinCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!JavaCompletionContributor.isInJavaContext(position)) {
            return
        }

        // Check if completing inside Mixin class
        val psiClass = position.findContainingClass() ?: return
        if (!psiClass.isMixin) {
            return
        }

        val superMixin = psiClass.superClass?.takeIf { it.isWritable && it.isMixin }

        val javaResult = JavaCompletionSorting.addJavaSorting(parameters, result)

        val filter = JavaCompletionContributor.getReferenceFilter(position)
        val prefixMatcher = result.prefixMatcher

        LegacyCompletionContributor.processReferences(parameters, javaResult) { reference, r ->
            if (reference !is PsiJavaReference) {
                // Only process references to Java elements
                return@processReferences
            }

            val start = if (reference is PsiQualifiedReference) {
                reference.qualifier?.let { qualifier ->
                    val qualifierExpression = qualifier as? PsiExpression ?: return@processReferences
                    when (qualifierExpression) {
                        // Usually, a qualified reference will be either "this" or "super"
                        is PsiThisExpression -> psiClass
                        is PsiSuperExpression -> superMixin ?: return@processReferences

                        else -> {
                            // As a fallback, we also support all other expressions
                            // However, it must point to another Mixin in the hierarchy (i.e. a super mixin)
                            val qualifierClass =
                                (qualifierExpression.type as? PsiClassType)?.resolve() ?: return@processReferences

                            // Quick check in case it's the current Mixin
                            if (qualifierClass equivalentTo psiClass) {
                                psiClass
                            } else {
                                // Qualifier class is valid if it's a Mixin and it's in our hierarchy
                                if (qualifierClass.isWritable && qualifierClass.isMixin && psiClass.isInheritor(
                                        qualifierClass,
                                        true
                                    )
                                ) {
                                    qualifierClass
                                } else {
                                    return@processReferences
                                }
                            }
                        }
                    }
                } ?: psiClass
            } else {
                psiClass
            }

            // Process methods and fields from target class
            findShadowTargets(psiClass, start, superMixin != null)
                .map { it.createLookupElement() }
                .filter { prefixMatcher.prefixMatches(it) }
                .filter(filter, position)
                .map { PrioritizedLookupElement.withExplicitProximity(it, 1) }
                .forEach(r::addElement)
        }
    }
}
