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

import com.google.common.collect.ImmutableSet
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiVariable
import com.intellij.psi.ResolveResult
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import org.jetbrains.annotations.Contract
import java.util.stream.Stream

@JvmOverloads
@Contract(value = "null, _ -> null", pure = true)
fun getClassOfElement(element: PsiElement?, resolveReferences: Boolean = false): PsiClass? {
    return findParent(element, resolveReferences)
}

@Contract(value = "null -> null", pure = true)
fun findReferencedMember(element: PsiElement?): PsiMember? {
    return findParent(element, true)
}

@Contract(value = "null -> null", pure = true)
inline fun <reified T : PsiElement> findParent(element: PsiElement?, resolveReferences: Boolean = false): T? {
    var el = element
    while (el != null) {
        if (resolveReferences && el is PsiReference) {
            el = el.resolve() ?: return null
        }

        if (el is T) {
            return el
        }

        if (el is PsiFile || el is PsiDirectory) {
            return null
        }

        el = el.parent
    }

    return null
}

inline fun <reified T : PsiElement> findChild(element: PsiElement): T? {
    val firstChild = element.firstChild ?: return null
    return findSibling(firstChild, false)
}

inline fun <reified T : PsiElement> findSibling(element: PsiElement, strict: Boolean = true): T? {
    var sibling = if (strict) element.nextSibling else element

    while (sibling != null) {
        if (sibling is T) {
            return sibling
        }

        sibling = sibling.nextSibling
    }

    return null
}

inline fun findLastChild(element: PsiElement, condition: (PsiElement) -> Boolean): PsiElement? {
    var child = element.firstChild
    var lastChild: PsiElement? = null

    while (child != null) {
        if (condition(child)) {
            lastChild = child
        }

        child = child.nextSibling
    }

    return lastChild
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
    return MEMBER_ACCESS_MODIFIERS.stream()
        .filter { member?.hasModifierProperty(it) == true }
        .findFirst()
        .orElse(PsiModifier.PUBLIC)
}

fun getAnnotation(owner: PsiModifierListOwner?, annotationName: String): PsiAnnotation? {
    if (owner == null) {
        return null
    }

    val list = owner.modifierList ?: return null

    return list.findAnnotation(annotationName)
}

internal fun <T : Any> Stream<T>.filter(filter: ElementFilter?, context: PsiElement): Stream<T> {
    filter ?: return this
    return filter { filter.isClassAcceptable(it.javaClass) && filter.isAcceptable(it, context) }
}

fun Stream<out PsiElement>.toResolveResults(): Array<ResolveResult> = map(::PsiElementResolveResult).toTypedArray()

internal fun PsiParameterList.synchronize(newParams: List<PsiParameter>) {
    ChangeSignatureUtil.synchronizeList(this, newParams, {it.parameters.asList()}, BooleanArray(newParams.size))
}

// PsiNameValuePair -> PsiAnnotationParameterList -> PsiAnnotation
internal val PsiElement.annotationFromNameValuePair
    get() = parent?.parent as? PsiAnnotation

// value -> PsiNameValuePair -> see above
internal val PsiElement.annotationFromValue
    get() = parent?.annotationFromNameValuePair

// value -> PsiArrayInitializerMemberValue -> PsiNameValuePair -> see above
internal val PsiElement.annotationFromArrayValue: PsiAnnotation?
    get() {
        val parent = parent ?: return null
        return if (parent is PsiArrayInitializerMemberValue) {
            parent.parent?.annotationFromNameValuePair
        } else {
            parent.annotationFromNameValuePair
        }
    }

internal val PsiElement.constantValue: Any
    get() = JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(this, true)

internal val PsiElement.constantStringValue: String
    get() = when (this) {
        is PsiLiteral -> value?.toString() ?: ""
        is PsiPolyadicExpression ->
            // We assume that the expression uses the '+' operator since that is the only valid one for constant expressions (of strings)
            operands.joinToString(separator = "", transform = PsiElement::constantStringValue)
        is PsiReferenceExpression ->
            // Possibly a reference to a constant field, attempt to resolve
            (resolve() as? PsiVariable)?.computeConstantValue()?.toString() ?: ""
        is PsiParenthesizedExpression ->
            // Useless parentheses? Fine with me!
            expression?.constantStringValue ?: ""
        is PsiTypeCastExpression ->
            // Useless type cast? Pfff.
            operand?.constantStringValue ?: ""
        else -> throw UnsupportedOperationException("Unsupported expression type: $this")
    }

internal fun PsiType.isErasureEquivalentTo(other: PsiType): Boolean {
    // TODO: Do more checks for generics instead
    return TypeConversionUtil.erasure(this) == TypeConversionUtil.erasure(other)
}
