/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.completion

import com.demonwav.mcdev.platform.mixin.reference.MixinReferences
import com.demonwav.mcdev.platform.mixin.util.findShadowTargets
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.filter
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.JavaCompletionContributor
import com.intellij.codeInsight.completion.JavaCompletionSorting
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.completion.LegacyCompletionContributor
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaReference
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.util.PsiTreeUtil

class MixinCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.completionType != CompletionType.BASIC) {
            return
        }

        val position = parameters.position
        if (!JavaCompletionContributor.isInJavaContext(position)) {
            return
        }

        val literal = PsiTreeUtil.getParentOfType(position, PsiLiteral::class.java)
        if (literal != null && MixinReferences.MIXIN_TARGETS.accepts(literal)) {
            provideMixinTargetsCompletion(parameters, result, literal)
            return
        }

        // Check if completing inside Mixin class
        val psiClass = position.findContainingClass() ?: return
        if (!psiClass.isMixin) {
            return
        }

        // Run all the other contributors first
        result.runRemainingContributors(parameters, result::passResult)

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
                                val isInheritor = psiClass.isInheritor(qualifierClass, true)

                                // Qualifier class is valid if it's a Mixin and it's in our hierarchy
                                if (qualifierClass.isWritable && qualifierClass.isMixin && isInheritor) {
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
            val elements = findShadowTargets(psiClass, start, superMixin != null)
                .filter {
                    val name = it.name
                    StringUtil.isJavaIdentifier(name) && prefixMatcher.prefixMatches(name)
                }
                .onEach { ProgressManager.checkCanceled() }
                .map { it.createLookupElement(psiClass.project) }
                .filter(filter, position)
                .toList()

            r.addAllElements(elements)
        }
    }

    private fun provideMixinTargetsCompletion(
        parameters: CompletionParameters,
        result: CompletionResultSet,
        literal: PsiLiteral,
    ) {
        val dummy = CompletionUtil.DUMMY_IDENTIFIER
        val dummyTrimmed = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
        val rawText = literal.constantStringValue ?: ""
        val text = rawText.removeSuffix(dummy).removeSuffix(dummyTrimmed)

        // If completing a bare class name (no package dot), run global class search
        if (!text.contains('.')) {
            AllClassesGetter.processJavaClasses(
                parameters,
                result.withPrefixMatcher(text).prefixMatcher,
                true,
            ) { cls ->
                val fqn = cls.qualifiedName ?: return@processJavaClasses
                val simpleName = cls.name ?: return@processJavaClasses
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        JavaLookupElementBuilder.forClass(cls, fqn, true)
                            .withLookupString(fqn)
                            .withLookupString(simpleName),
                        0.5,
                    ),
                )
            }
        }
    }
}
