/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.platform.mixin.util.findFields
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.util.getClassOfElement
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.JavaCompletionContributor
import com.intellij.codeInsight.completion.JavaCompletionSorting
import com.intellij.codeInsight.completion.LegacyCompletionContributor
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.scope.JavaCompletionProcessor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.ResolveState

class MixinCompletionContributor : CompletionContributor() {

    private val COMPLETION_OPTIONS: JavaCompletionProcessor.Options = JavaCompletionProcessor.Options.DEFAULT_OPTIONS.withCheckAccess(false)

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!JavaCompletionContributor.isInJavaContext(position)) {
            return
        }

        // Check if completing inside Mixin class
        val psiClass = getClassOfElement(position) ?: return
        val targets = MixinUtils.getAllMixedClasses(psiClass).values
        if (targets.isEmpty()) {
            return
        }

        val javaResult = JavaCompletionSorting.addJavaSorting(parameters, result)

        val targetType = JavaPsiFacade.getElementFactory(psiClass.project).createType(psiClass)
        val prefixMatcher = result.prefixMatcher
        val filter = JavaCompletionContributor.getReferenceFilter(position) ?: return
        LegacyCompletionContributor.processReferences(parameters, javaResult, { reference, result ->
            // Check if referenced element is our Mixin class
            val qualified = if (reference is PsiQualifiedReference && reference.qualifier != null) {
                val qualifierExpression = reference.qualifier as? PsiExpression ?: return@processReferences
                val type = qualifierExpression.type ?: return@processReferences
                if (type != targetType) {
                    return@processReferences
                }

                true
            } else {
                false
            }

            // Collect completions from target class(es)
            val processor = JavaCompletionProcessor(position, filter, COMPLETION_OPTIONS, prefixMatcher::prefixMatches)

            // Process methods and fields from target class
            findMethods(psiClass, targets)?.forEach {
                processor.execute(it, ResolveState.initial())
            }
            findFields(psiClass, targets)?.forEach {
                processor.execute(it, ResolveState.initial())
            }

            // Add lookups for processed completions
            for (completion in processor.results) {
                val element = completion.element

                result.addElement(PrioritizedLookupElement.withExplicitProximity(when (element) {
                    is PsiMethod -> MixinMethodLookupItem(element)
                    is PsiField -> MixinFieldLookupItem(element, qualified)
                    else -> throw AssertionError("Member is not PsiMethod or PsiField")
                }, 1))
            }
        })
    }

}
