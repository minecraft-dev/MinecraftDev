/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.INJECTION_POINT
import com.demonwav.mcdev.util.toArray
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.ProcessingContext

// TODO: The stuff here wouldn't mind some caching

internal class MixinInjectionPointTypeReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val project = element.project
        val baseClass = JavaPsiFacade.getInstance(project)
                .findClass(INJECTION_POINT, GlobalSearchScope.allScope(project)) ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(InjectionPointTypeReference(element as PsiLiteral, baseClass))
    }

}

private class InjectionPointTypeReference(element: PsiLiteral, val baseClass: PsiClass) :
        PsiReferenceBase<PsiLiteral>(element), MixinReference {

    override val description
        get() = "injection point type '$value'"

    val injectionPointTypes: Map<String, PsiClass> by lazy {
        val map = hashMapOf<String, PsiClass>()
        for (c in ClassInheritorsSearch.search(baseClass)) {
            val code = c.findFieldByName("CODE", false)?.computeConstantValue() as? String
            if (code != null) {
                map.put(code, c)
            }
        }

        map
    }

    override fun resolve() = injectionPointTypes[value]

    override fun getVariants() = injectionPointTypes.keys.toArray()

}
