/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McPsiUtil")
package com.demonwav.mcdev.util

import com.google.common.collect.ImmutableSet
import com.google.common.collect.Lists
import com.intellij.navigation.AnonymousElementProvider
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import org.jetbrains.annotations.Contract
import java.util.Collections
import java.util.stream.Collectors
import java.util.stream.Stream

@JvmOverloads
@Contract(value = "null, _ -> null", pure = true)
fun getClassOfElement(element: PsiElement?, resolveReferences: Boolean = false): PsiClass? {
    return findElement(element, { it as? PsiClass }, resolveReferences)
}

fun findReferencedMember(element: PsiElement?): PsiMember? {
    return findElement(element, { it as? PsiMember }, true)
}

@Contract(value = "null -> null", pure = true)
private inline fun <T : PsiElement> findElement(element: PsiElement?, func: (PsiElement) -> T?, resolveReferences: Boolean): T? {
    var el = element
    while (el != null) {
        if (resolveReferences && el is PsiReference) {
            el = el.resolve() ?: return null
        }

        val result = func(el)
        if (result != null) {
            return result
        }

        if (el is PsiFile || el is PsiDirectory) {
            return null
        }

        el = el.parent
    }

    return null
}

fun extendsOrImplementsClass(psiClass: PsiClass, qualifiedClassName: String): Boolean {
    val project = psiClass.project
    val aClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, GlobalSearchScope.allScope(project))

    return aClass != null && psiClass.isInheritor(aClass, true)
}

fun addImplements(psiClass: PsiClass, qualifiedClassName: String, project: Project) {
    val referenceList = psiClass.implementsList
    val listenerClass = JavaPsiFacade.getInstance(project).findClass(qualifiedClassName, GlobalSearchScope.allScope(project))
    if (listenerClass != null) {
        val element = JavaPsiFacade.getElementFactory(project).createClassReferenceElement(listenerClass)
        if (referenceList != null) {
            referenceList.add(element)
        } else {
            val list = JavaPsiFacade.getElementFactory(project).createReferenceList(arrayOf(element))
            psiClass.add(list)
        }
    }
}

private val MEMBER_ACCESS_MODIFIERS = ImmutableSet.builder<String>()
    .add(PsiModifier.PUBLIC)
    .add(PsiModifier.PROTECTED)
    .add(PsiModifier.PACKAGE_LOCAL)
    .add(PsiModifier.PRIVATE)
    .build()

fun getAccessModifier(member: PsiMember?): String {
    return if (member != null)
        MEMBER_ACCESS_MODIFIERS.stream()
            .filter { member.hasModifierProperty(it) }
            .findFirst()
            .orElse(PsiModifier.PUBLIC)
    else
        PsiModifier.PUBLIC
}

fun getAnnotation(owner: PsiModifierListOwner?, annotationName: String): PsiAnnotation? {
    if (owner == null) {
        return null
    }

    val list = owner.modifierList ?: return null

    return list.findAnnotation(annotationName)
}

@Contract(value = "null -> null", pure = true)
fun getNameOfClass(psiClass: PsiClass?): Pair<String, PsiClass>? {
    var aClass = psiClass ?: return null

    if (aClass.containingClass == null) {
        return Pair.create<String, PsiClass>("", psiClass)
    }

    val innerStrings = Lists.newArrayList<String>()
    var baseClass: PsiClass = psiClass
    while (aClass != null) {
        baseClass = aClass
        if (aClass.name == null) {
            // anon class
            var anonymousClasses: Array<PsiElement>? = null
            for (provider in Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {

                anonymousClasses = provider.getAnonymousElements(psiClass.containingClass!!)
                if (anonymousClasses.isNotEmpty()) {
                    break
                }
            }

            if (anonymousClasses == null) {
                // We couldn't build the proper string, so don't return anything at all
                return null
            }

            for (i in anonymousClasses.indices) {
                if (anonymousClasses[i] === psiClass) {
                    innerStrings.add((i + 1).toString())
                    break
                }
            }
        } else {
            innerStrings.add(aClass.name)
            aClass = aClass.containingClass as PsiClass
        }
    }

    // We started from the bottom and went up, so reverse it
    Collections.reverse(innerStrings)
    // Skip the base class, we are giving the base PsiClass so the user can do with it what they want
    return Pair.create("$" + innerStrings.stream().skip(1).collect(Collectors.joining("$")), baseClass)
}

fun createResolveResults(elements: Stream<out PsiElement>): Array<ResolveResult> {
    return elements
            .map(::PsiElementResolveResult)
            .toArray({ size -> arrayOfNulls<ResolveResult>(size) })
}

fun PsiParameterList.synchronize(newParams: List<PsiParameter>) {
    ChangeSignatureUtil.synchronizeList(this, newParams, {it.parameters.asList()}, BooleanArray(newParams.size))
}
