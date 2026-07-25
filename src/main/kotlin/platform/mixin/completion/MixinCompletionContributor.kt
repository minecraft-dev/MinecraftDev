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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.findShadowTargets
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.filter
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.JavaCompletionContributor
import com.intellij.codeInsight.completion.JavaCompletionSorting
import com.intellij.codeInsight.completion.LegacyCompletionContributor
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaReference
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons

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

        // Check if completing inside @Mixin targets attribute
        if (isInsideMixinTargets(position)) {
            provideMixinTargetsCompletion(position, result)
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

    private fun isInsideMixinTargets(position: PsiElement): Boolean {
        val literal = PsiTreeUtil.getParentOfType(position, PsiLiteral::class.java) ?: return false
        if (literal.value !is String) return false
        var current = literal.parent
        while (current != null) {
            when (current) {
                is PsiNameValuePair -> {
                    return current.attributeName == "targets" &&
                        current.parent?.parent is PsiAnnotation &&
                        (current.parent.parent as PsiAnnotation).qualifiedName == MIXIN
                }

                is PsiAnnotation -> return false
                is PsiClass -> return false
            }
            current = current.parent
        }
        return false
    }

    private fun provideMixinTargetsCompletion(position: PsiElement, result: CompletionResultSet) {
        val literal = PsiTreeUtil.getParentOfType(position, PsiLiteral::class.java) ?: return
        val text = literal.constantStringValue?.removeSuffix(CompletionUtil.DUMMY_IDENTIFIER) ?: ""

        val project = position.project
        val scope = position.resolveScope
        val facade = JavaPsiFacade.getInstance(project)

        val parts = text.split('.').dropLast(1)
        val packageName = parts.joinToString(".")

        // Show packages and classes from the parent package
        val pkg = facade.findPackage(packageName.ifEmpty { "" })
        if (pkg != null) {
            for (subPkg in pkg.getSubPackages(scope)) {
                val subPkgName = subPkg.qualifiedName
                val subPkgSimpleName = subPkg.name ?: continue
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(subPkgName)
                            .withPresentableText(subPkgSimpleName)
                            .withIcon(PlatformIcons.PACKAGE_ICON),
                        1.0,
                    ),
                )
            }
            for (cls in pkg.getClasses(scope)) {
                val fqn = cls.qualifiedName ?: continue
                val simpleName = fqn.substringAfterLast('.')
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(fqn)
                            .withPresentableText(simpleName)
                            .withLookupString(simpleName)
                            .withIcon(cls.getIcon(0)),
                        0.5,
                    ),
                )
            }
        }

        // When input is empty, also show all class names (limited)
        val cache = PsiShortNamesCache.getInstance(project)
        if (packageName.isEmpty() && text.firstOrNull()?.isUpperCase() == true) {
            for (className in cache.allClassNames) {
                for (cls in cache.getClassesByName(className, scope)) {
                    val fqn = cls.qualifiedName ?: continue
                    val simpleName = fqn.substringAfterLast('.')
                    result.addElement(
                        PrioritizedLookupElement.withPriority(
                            LookupElementBuilder.create(fqn)
                                .withPresentableText(simpleName)
                                .withLookupString(simpleName)
                                .withIcon(cls.getIcon(0)),
                            0.5,
                        ),
                    )
                }
            }
        }
    }
}
