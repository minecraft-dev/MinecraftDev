/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util.reference

import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.packageName
import com.intellij.codeInsight.completion.JavaClassNameCompletionContributor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPackage
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PackageScope
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons

abstract class ClassNameReferenceProvider : PackageNameReferenceProvider() {

    override val description: String
        get() = "class/package '%s'"

    protected abstract fun findClasses(element: PsiElement, scope: GlobalSearchScope): List<PsiClass>

    override fun canBindTo(element: PsiElement) = super.canBindTo(element) || element is PsiClass

    override fun resolve(qualifiedName: String, element: PsiElement, facade: JavaPsiFacade): Array<ResolveResult> {
        val classes = facade.findClasses(qualifiedName, element.resolveScope)
        if (classes.isNotEmpty()) {
            return classes.mapToArray(::PsiElementResolveResult)
        }

        return super.resolve(qualifiedName, element, facade)
    }

    override fun collectVariants(element: PsiElement, context: PsiElement?): Array<Any> {
        return if (context != null) {
            if (context is PsiPackage) {
                collectSubpackages(element, context)
            } else {
                ArrayUtil.EMPTY_OBJECT_ARRAY // TODO: Add proper support for inner classes
            }
        } else {
            collectClasses(element)
        }
    }

    private fun collectClasses(element: PsiElement): Array<Any> {
        val classes = findClasses(element, element.resolveScope).ifEmpty { return ArrayUtil.EMPTY_OBJECT_ARRAY }

        val list = ArrayList<Any>()
        val packages = HashSet<String>()

        val basePackage = getBasePackage(element)

        for (psiClass in classes) {
            list.add(JavaClassNameCompletionContributor.createClassLookupItem(psiClass, false))

            val topLevelPackage = getTopLevelPackageName(psiClass, basePackage) ?: continue
            if (packages.add(topLevelPackage)) {
                list.add(LookupElementBuilder.create(topLevelPackage).withIcon(PlatformIcons.PACKAGE_ICON))
            }
        }

        return list.toArray()
    }

    private fun getTopLevelPackageName(psiClass: PsiClass, basePackage: String?): String? {
        val packageName = psiClass.packageName ?: return null
        val start = if (basePackage != null && packageName.startsWith(basePackage)) {
            if (packageName.length == basePackage.length) {
                // packageName == basePackage
                return null
            }

            basePackage.length + 1
        } else 0

        val end = packageName.indexOf('.', start)
        return if (end == -1) {
            packageName.substring(start)
        } else {
            packageName.substring(start, end)
        }
    }

    private fun collectSubpackages(element: PsiElement, context: PsiPackage): Array<Any> {
        val classes = findClasses(element, PackageScope(context, true, true).intersectWith(element.resolveScope))
            .ifEmpty { return ArrayUtil.EMPTY_OBJECT_ARRAY }
        return collectPackageChildren(context, classes) {
            JavaClassNameCompletionContributor.createClassLookupItem(it, false)
        }
    }
}
