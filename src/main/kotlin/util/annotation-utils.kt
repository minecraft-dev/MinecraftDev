/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType
import com.intellij.psi.codeStyle.JavaCodeStyleManager

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

    return (operand.type as? PsiClassType)?.resolve()
}

fun PsiAnnotationMemberValue.resolveTypeArray(): List<PsiType> {
    return parseArray { it.resolveType() }
}

fun PsiAnnotationMemberValue.resolveType(): PsiType? {
    return (this as? PsiClassObjectAccessExpression)?.operand?.type
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

inline fun <T : Any> PsiAnnotationMemberValue.parseArray(func: (PsiAnnotationMemberValue) -> T?): List<T> {
    return if (this is PsiArrayInitializerMemberValue) {
        initializers.mapNotNull(func)
    } else {
        return listOfNotNull(func(this))
    }
}

// PsiNameValuePair -> PsiAnnotationParameterList -> PsiAnnotation
val PsiElement.annotationFromNameValuePair
    get() = parent?.parent as? PsiAnnotation

// value -> PsiNameValuePair -> see above
val PsiElement.annotationFromValue
    get() = parent?.annotationFromNameValuePair

// value -> PsiArrayInitializerMemberValue -> PsiNameValuePair -> see above
val PsiElement.annotationFromArrayValue: PsiAnnotation?
    get() {
        val parent = parent ?: return null
        return if (parent is PsiArrayInitializerMemberValue) {
            parent.parent?.annotationFromNameValuePair
        } else {
            parent.annotationFromNameValuePair
        }
    }

fun PsiModifierListOwner.addAnnotation(annotationText: String): PsiAnnotation? {
    val annotation = JavaPsiFacade.getElementFactory(this.project).createAnnotationFromText(annotationText, this)
    return this.addAnnotation(annotation)
}

fun PsiModifierListOwner.addAnnotation(annotation: PsiAnnotation): PsiAnnotation? {
    val modifierList = this.modifierList ?: return null
    val fqn = annotation.qualifiedName ?: return null
    val inserted = modifierList.addAnnotation(fqn)
    for (pair in annotation.parameterList.attributes) {
        inserted.setDeclaredAttributeValue(pair.name, pair.value)
    }

    JavaCodeStyleManager.getInstance(project).shortenClassReferences(inserted)
    return inserted
}
