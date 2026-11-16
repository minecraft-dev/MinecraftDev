/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.util.InjectionPointSpecifier
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT_CODE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.INJECTION_POINT
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.reference.ReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.parentOfType

object InjectionPointReference : ReferenceResolver(), MixinReference {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(AT)

    override fun unresolvedMessage(name: String?) = MCDevBundle("reference.unresolved.injection_point", name)

    override fun isValidAnnotation(name: String, project: Project) = name == AT

    override fun resolveReference(context: PsiElement): PsiElement? {
        // Remove slice selectors from the injection point type
        var name = context.constantStringValue ?: return null
        if (allowSpecifiers(context)) {
            for (specifier in InjectionPointSpecifier.entries) {
                if (name.endsWith(":$specifier")) {
                    name = name.substringBeforeLast(':')
                    break
                }
            }
        }

        // find the name if it's an at code
        getAllAtCodes(context.project)[name]?.let { return it }

        // find a fully qualified class name that extends InjectionPoint
        val psiClass = JavaPsiFacade.getInstance(context.project)
            .findClass(name, GlobalSearchScope.allScope(context.project)) ?: return null
        return psiClass.takeIf { InheritanceUtil.isInheritor(it, INJECTION_POINT) }
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        return resolveReference(context) == null
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val atCodes = getAllAtCodes(context.project).keys
        val text = context.constantStringValue?.removeSuffix(CompletionUtil.DUMMY_IDENTIFIER)
        val prefix = text?.substringBeforeLast(':', missingDelimiterValue = "")
        if (prefix != null && prefix in atCodes && allowSpecifiers(context)) {
            return InjectionPointSpecifier.entries.asSequence()
                .map { LookupElementBuilder.create("$prefix:$it") }
                .toTypedArray()
        }
        return (
            atCodes.asSequence()
                .filter {
                    InjectionPoint.byAtCode(it)?.discouragedMessage == null
                }
                .map {
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(it).completeInjectionPoint(context),
                        1.0,
                    )
                } +
                getCustomInjectionPointInheritors(context.project).asSequence()
                    .map {
                        PrioritizedLookupElement.withPriority(
                            LookupElementBuilder.create(it).completeInjectionPoint(context),
                            0.0,
                        )
                    }
            ).toTypedArray()
    }

    private fun allowSpecifiers(context: PsiElement): Boolean {
        val at = context.parentOfType<PsiAnnotation>() ?: return false
        return InjectionPointSpecifier.isAllowed(at)
    }

    private fun LookupElementBuilder.completeInjectionPoint(context: PsiElement): LookupElementBuilder {
        val injectionPoint = InjectionPoint.byAtCode(lookupString) ?: return completeToLiteral(context)

        return completeToLiteral(context) { editor, element ->
            injectionPoint.onCompleted(editor, element)
        }
    }

    private val INJECTION_POINT_INHERITORS = Key<CachedValue<List<String>>>("mcdev.injectionPointInheritors")

    private fun getCustomInjectionPointInheritors(project: Project): List<String> {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            INJECTION_POINT_INHERITORS,
            {
                val injectionPointClass = JavaPsiFacade.getInstance(project)
                    .findClass(INJECTION_POINT, GlobalSearchScope.allScope(project))
                    ?: return@getCachedValue CachedValueProvider.Result(
                        emptyList(),
                        PsiModificationTracker.MODIFICATION_COUNT,
                    )
                val inheritors = ClassInheritorsSearch.search(injectionPointClass).mapNotNull { c ->
                    if (c.qualifiedName == INJECTION_POINT) return@mapNotNull null

                    // filter out builtin mixin injection points
                    if (c.qualifiedName?.startsWith("org.spongepowered.asm.mixin.injection.") == true) {
                        return@mapNotNull null
                    }

                    // filter out injection points with at codes, they're already listed
                    c.getAnnotation(AT_CODE)?.let { return@mapNotNull null }

                    c.qualifiedName
                }
                CachedValueProvider.Result(inheritors, PsiModificationTracker.MODIFICATION_COUNT)
            },
            false,
        )
    }

    private fun getAllAtCodes(project: Project): Map<String, PsiClass> {
        val atCode = JavaPsiFacade.getInstance(project).findClass(AT_CODE, GlobalSearchScope.allScope(project))
            ?: return emptyMap()
        return atCode.cached(PsiModificationTracker.MODIFICATION_COUNT) {
            AnnotatedElementsSearch.searchPsiClasses(atCode, GlobalSearchScope.allScope(project)).asSequence()
                .mapNotNull { c ->
                    val atCodeAnnotation = c.getAnnotation(AT_CODE) ?: return@mapNotNull null
                    val value = atCodeAnnotation.findDeclaredAttributeValue("value")?.constantStringValue
                        ?: return@mapNotNull null
                    val namespace = atCodeAnnotation.findDeclaredAttributeValue("namespace")
                        ?.constantStringValue
                        ?: ""
                    if (namespace.isEmpty()) {
                        value
                    } else {
                        "$namespace:$value"
                    } to c
                }
                .toMap()
        }
    }
}
