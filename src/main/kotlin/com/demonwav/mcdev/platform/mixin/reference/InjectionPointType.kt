/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT_CODE
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.INJECTION_POINT
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.reference.ReferenceResolver
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch

object InjectionPointType : ReferenceResolver(), MixinReference {

    override val description: String
        get() = "injection point type '%s'"

    override fun isValidAnnotation(name: String) = name == AT

    override fun resolveReference(context: PsiElement): PsiElement? {
        // Remove selectors from the injection point type for now
        // TODO: Remove this when we have full support for @Slices
        val value = context.constantStringValue?.substringBefore(':') ?: return null
        findTypes(context) { code, psiClass ->
            if (value == code) {
                return psiClass
            }
        }
        return null
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        return resolveReference(context) == null
    }

    override fun collectVariants(context: PsiElement): Array<Any> {
        val list = ArrayList<LookupElementBuilder>()
        findTypes(context) { code, _ ->
            list.add(LookupElementBuilder.create(code).completeToLiteral(context))
        }
        return list.toArray()
    }

    private inline fun findTypes(context: PsiElement, consume: (String, PsiClass) -> Unit) {
        // TODO: Caching wouldn't hurt here
        val atCode = JavaPsiFacade.getInstance(context.project).findClass(AT_CODE, context.resolveScope)
        if (atCode != null) {
            // Mixin 0.6.5+, using @AtCode annotation
            for (c in AnnotatedElementsSearch.searchPsiClasses(atCode, context.resolveScope)) {
                c.modifierList!!.findAnnotation(AT_CODE)!!.findDeclaredAttributeValue("value")?.constantStringValue
                    ?.let { code -> consume(code, c) }
            }
        } else {
            // Older Mixin version. Resolve based on inheritors of InjectionPoint with a static final "CODE" field
            val baseClass =
                JavaPsiFacade.getInstance(context.project).findClass(INJECTION_POINT, context.resolveScope) ?: return
            for (c in ClassInheritorsSearch.search(baseClass)) {
                (c.findFieldByName("CODE", false)?.computeConstantValue() as? String)
                    ?.let { code -> consume(code, c) }
            }
        }
    }
}
