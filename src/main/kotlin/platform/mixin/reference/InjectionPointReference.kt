/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT_CODE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.INJECTION_POINT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.SELECTOR
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.reference.ReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
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

    override val description: String
        get() = "injection point type '%s'"

    override fun isValidAnnotation(name: String, project: Project) = name == AT

    override fun resolveReference(context: PsiElement): PsiElement? {
        // Remove slice selectors from the injection point type
        var name = context.constantStringValue ?: return null
        val at = context.parentOfType<PsiAnnotation>() ?: return null
        val isInsideSlice = at.parentOfType<PsiAnnotation>()?.hasQualifiedName(SLICE) == true
        if (isInsideSlice) {
            for (sliceSelector in getSliceSelectors(context.project)) {
                if (name.endsWith(":$sliceSelector")) {
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
        return (
            getAllAtCodes(context.project).keys.asSequence()
                .map {
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(it).completeToLiteral(context),
                        1.0
                    )
                } +
                getCustomInjectionPointInheritors(context.project).asSequence()
                    .map {
                        PrioritizedLookupElement.withPriority(
                            LookupElementBuilder.create(it).completeToLiteral(context),
                            0.0
                        )
                    }
            ).toTypedArray()
    }

    private val SLICE_SELECTORS_KEY = Key<CachedValue<List<String>>>("mcdev.sliceSelectors")

    private fun getSliceSelectors(project: Project): List<String> {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            SLICE_SELECTORS_KEY,
            {
                val selectorClass = JavaPsiFacade.getInstance(project)
                    .findClass(SELECTOR, GlobalSearchScope.allScope(project))
                    ?: return@getCachedValue CachedValueProvider.Result(
                        emptyList(),
                        PsiModificationTracker.MODIFICATION_COUNT
                    )
                val enumConstants = selectorClass.fields.mapNotNull { (it as? PsiEnumConstant)?.name }
                CachedValueProvider.Result(enumConstants, PsiModificationTracker.MODIFICATION_COUNT)
            },
            false
        )
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
                        PsiModificationTracker.MODIFICATION_COUNT
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
            false
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
