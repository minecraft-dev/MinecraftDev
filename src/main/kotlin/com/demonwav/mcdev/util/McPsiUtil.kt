/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McPsiUtil")
package com.demonwav.mcdev.util

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveResult
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import org.jetbrains.annotations.Contract
import java.util.stream.Stream

// Parent

@Contract(pure = true)
fun PsiElement.findContainingClass(): PsiClass? = findParent(resolveReferences = false)

@Contract(pure = true)
fun PsiElement.findReferencedClass(): PsiClass? = findParent(resolveReferences = true)

@Contract(pure = true)
fun PsiElement.findReferencedMember(): PsiMember? = findParent({ it is PsiClass }, resolveReferences = true)

@Contract(pure = true)
fun PsiElement.findContainingMethod(): PsiMethod? = findParent({ it is PsiClass }, resolveReferences = false)

@Contract(pure = true)
fun PsiElement.findJavaCodeReferenceElement(): PsiJavaCodeReferenceElement? {
    return findParent({ it is PsiMethod || it is PsiClass }, resolveReferences = false)
}

@Contract(pure = true)
private inline fun <reified T : PsiElement> PsiElement.findParent(resolveReferences: Boolean): T? {
    return findParent({false}, resolveReferences)
}

@Contract(pure = true)
private inline fun <reified T : PsiElement> PsiElement.findParent(stop: (PsiElement) -> Boolean, resolveReferences: Boolean): T? {
    var el: PsiElement = this

    while (true) {
        if (resolveReferences && el is PsiReference) {
            el = el.resolve() ?: return null
        }

        if (el is T) {
            return el
        }

        if (el is PsiFile || el is PsiDirectory || stop(el)) {
            return null
        }

        el = el.parent ?: return null
    }
}

// Children
@Contract(pure = true)
fun PsiClass.findFirstMember(): PsiMember? = findChild()

@Contract(pure = true)
fun PsiElement.findNextMember(): PsiMember? = findSibling(true)

@Contract(pure = true)
private inline fun <reified T : PsiElement> PsiElement.findChild(): T? {
    return firstChild?.findSibling(strict = false)
}

@Contract(pure = true)
private inline fun <reified T : PsiElement> PsiElement.findSibling(strict: Boolean): T? {
    var sibling = if (strict) nextSibling ?: return null else this
    while (true) {
        if (sibling is T) {
            return sibling
        }

        sibling = sibling.nextSibling ?: return null
    }
}

@Contract(pure = true)
inline fun PsiElement.findLastChild(condition: (PsiElement) -> Boolean): PsiElement? {
    var child = firstChild ?: return null
    var lastChild: PsiElement? = null

    while (true) {
        if (condition(child)) {
            lastChild = child
        }

        child = child.nextSibling ?: return lastChild
    }
}

@Contract(pure = true)
fun <T : Any> Stream<T>.filter(filter: ElementFilter?, context: PsiElement): Stream<T> {
    filter ?: return this
    return filter { filter.isClassAcceptable(it.javaClass) && filter.isAcceptable(it, context) }
}

@Contract(pure = true)
fun Stream<out PsiElement>.toResolveResults(): Array<ResolveResult> = map(::PsiElementResolveResult).toTypedArray()

fun PsiParameterList.synchronize(newParams: List<PsiParameter>) {
    ChangeSignatureUtil.synchronizeList(this, newParams, {it.parameters.asList()}, BooleanArray(newParams.size))
}

@get:Contract(pure = true)
val PsiElement.constantValue: Any?
    get() = JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(this)

@get:Contract(pure = true)
val PsiElement.constantStringValue: String?
    get() = constantValue as? String

@Contract(pure = true)
fun PsiType.isErasureEquivalentTo(other: PsiType): Boolean {
    // TODO: Do more checks for generics instead
    return TypeConversionUtil.erasure(this) == TypeConversionUtil.erasure(other)
}
