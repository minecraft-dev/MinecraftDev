/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.annotations.Contract

fun PsiModifierListOwner.findAnnotation(qualifiedName: String): PsiAnnotation? {
    return modifierList?.findAnnotation(qualifiedName)
}

fun PsiAnnotationMemberValue.findAnnotations(): List<PsiAnnotation> {
    return parseArray { it as? PsiAnnotation }
}

fun PsiAnnotationMemberValue.computeStringArray(): List<String> {
    return parseArray { it.constantStringValue }
}

fun PsiAnnotationMemberValue.resolveClassArray(): List<PsiClass> {
    return parseArray { it.resolveClass() }
}

fun PsiAnnotationMemberValue.resolveClass(): PsiClass? {
    if (this !is PsiClassObjectAccessExpression) {
        return null
    }

    return (operand.type as PsiClassType).resolve()
}

/**
 * Returns `true` if the annotation value is present (not `null`) and
 * initialized either to a single value or an array with at least one
 * element.
 *
 * @return `true` if the annotation member is not empty
 */
fun PsiAnnotationMemberValue?.isNotEmpty(): Boolean {
    return this != null && (this !is PsiArrayInitializerMemberValue || initializers.isNotEmpty())
}

private inline fun <T : Any> PsiAnnotationMemberValue.parseArray(func: (PsiAnnotationMemberValue) -> T?): List<T> {
    return if (this is PsiArrayInitializerMemberValue) {
        initializers.mapNotNull(func)
    } else {
        return listOfNotNull(func(this))
    }
}

// PsiNameValuePair -> PsiAnnotationParameterList -> PsiAnnotation
@get:Contract(pure = true)
val PsiElement.annotationFromNameValuePair
    get() = parent?.parent as? PsiAnnotation

// value -> PsiNameValuePair -> see above
@get:Contract(pure = true)
val PsiElement.annotationFromValue
    get() = parent?.annotationFromNameValuePair

// value -> PsiArrayInitializerMemberValue -> PsiNameValuePair -> see above
@get:Contract(pure = true)
val PsiElement.annotationFromArrayValue: PsiAnnotation?
    get() {
        val parent = parent ?: return null
        return if (parent is PsiArrayInitializerMemberValue) {
            parent.parent?.annotationFromNameValuePair
        } else {
            parent.annotationFromNameValuePair
        }
    }
