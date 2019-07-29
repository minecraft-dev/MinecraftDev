/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config.reference

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.util.reference.ClassNameReferenceProvider
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PackageScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

object MixinClass : ClassNameReferenceProvider() {

    override fun getBasePackage(element: PsiElement): String? {
        // Literal -> Array -> Property -> Object
        val obj = element.parent?.parent?.parent as? JsonObject ?: return null
        return (obj.findProperty("package")?.value as? JsonStringLiteral)?.value
    }

    override fun findClasses(element: PsiElement, scope: GlobalSearchScope): List<PsiClass> {
        val facade = JavaPsiFacade.getInstance(element.project)
        val mixinAnnotation = facade.findClass(MIXIN, element.resolveScope) ?: return emptyList()

        val packageScope = getBasePackage(element)?.let { facade.findPackage(it) }
            ?.let { scope.intersectWith(PackageScope(it, true, true)) } ?: scope
        return AnnotatedElementsSearch.searchPsiClasses(mixinAnnotation, packageScope).toList()
    }
}
