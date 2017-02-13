/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.platform.mixin.util.findFields
import com.demonwav.mcdev.platform.mixin.util.findMethods
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
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaReference
import com.intellij.psi.PsiQualifiedReference
import java.util.stream.Stream

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
        val targets = MixinUtils.getAllMixedClasses(psiClass).values
        if (targets.isEmpty()) {
            return
        }

        val javaResult = JavaCompletionSorting.addJavaSorting(parameters, result)

        val targetType = JavaPsiFacade.getElementFactory(psiClass.project).createType(psiClass)
        val filter = JavaCompletionContributor.getReferenceFilter(position)
        val prefixMatcher = result.prefixMatcher
        LegacyCompletionContributor.processReferences(parameters, javaResult, { reference, result ->
            if (reference !is PsiJavaReference) {
                // Only process references to Java elements
                return@processReferences
            }

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

            // Process methods and fields from target class
            Stream.concat(
                    findMethods(psiClass, targets, checkBases = true)?.map(::MixinMethodLookupItem) ?: Stream.empty<LookupElement>(),
                    findFields(psiClass, targets, checkBases = true)?.map({ MixinFieldLookupItem(it, qualified) }) ?: Stream.empty<LookupElement>())
                    .filter(prefixMatcher::prefixMatches)
                    .filter(filter, position)
                    .map { PrioritizedLookupElement.withExplicitProximity(it, 1) }
                    .forEach(result::addElement)
        })
    }

}
