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
import com.demonwav.mcdev.util.packageName
import com.demonwav.mcdev.util.reference.PackageNameReferenceProvider
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.PackageScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons

object MixinPackage : PackageNameReferenceProvider() {

    override fun collectVariants(element: PsiElement, context: PsiElement?): Array<Any> {
        val mixinAnnotation = JavaPsiFacade.getInstance(element.project).findClass(MIXIN, element.resolveScope)
            ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        return if (context == null) {
            findPackages(mixinAnnotation, element)
        } else {
            findChildrenPackages(mixinAnnotation, element, context as PsiPackage)
        }
    }

    private fun findPackages(mixinAnnotation: PsiClass, element: PsiElement): Array<Any> {
        val packages = HashSet<String>()
        val list = ArrayList<LookupElementBuilder>()

        for (mixin in AnnotatedElementsSearch.searchPsiClasses(mixinAnnotation, element.resolveScope)) {
            val packageName = mixin.packageName ?: continue
            if (packages.add(packageName)) {
                list.add(LookupElementBuilder.create(packageName).withIcon(PlatformIcons.PACKAGE_ICON))
            }

            val topLevelPackage = packageName.substringBefore('.')
            if (packages.add(topLevelPackage)) {
                list.add(LookupElementBuilder.create(topLevelPackage).withIcon(PlatformIcons.PACKAGE_ICON))
            }
        }

        return list.toArray()
    }

    private fun findChildrenPackages(mixinAnnotation: PsiClass, element: PsiElement, context: PsiPackage): Array<Any> {
        val scope = PackageScope(context, true, true).intersectWith(element.resolveScope)
        return collectSubpackages(context, AnnotatedElementsSearch.searchPsiClasses(mixinAnnotation, scope))
    }
}
