/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McPsiClass")
package com.demonwav.mcdev.util

import com.intellij.navigation.AnonymousElementProvider
import com.intellij.openapi.extensions.Extensions
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

private const val CLASS_SEPARATOR = '.'
private const val INNER_CLASS_SEPARATOR = '$'

val PsiClass.qualifiedJavaName: String
    get() = getQualifiedJavaName()

fun PsiClass.getQualifiedJavaName(separator: Char = CLASS_SEPARATOR): String {
    // Check if class is an inner class, otherwise just return the qualified name (replace separator if needed)
    val parentClass = containingClass ?:
            return if (separator == '.') qualifiedName!! else qualifiedName!!.replace(CLASS_SEPARATOR, separator)

    // Build the name for the inner class
    return buildQualifiedInnerName(StringBuilder(), parentClass, separator).toString()
}

fun PsiClass.appendQualifiedJavaName(builder: StringBuilder, separator: Char = CLASS_SEPARATOR): StringBuilder {
    // Check if class is an inner class, otherwise just append the qualified name (replace separator if needed)
    val parentClass = containingClass ?:
            return if (separator == '.') builder.append(qualifiedName!!) else builder.append(qualifiedName!!.replace(CLASS_SEPARATOR, separator))

    // Build the name for the inner class
    return buildQualifiedInnerName(builder, parentClass, separator)
}

private fun PsiClass.buildQualifiedInnerName(builder: StringBuilder, parentClass: PsiClass, separator: Char): StringBuilder {
    // Recursively append parent class names
    parentClass.appendQualifiedJavaName(builder, separator)

    val name = name
    if (name != null) {
        return builder.append(INNER_CLASS_SEPARATOR).append(name)
    }

    // Attempt to find name for anonymous class
    for ((i, element) in anonymousElements!!.withIndex()) {
        if (manager.areElementsEquivalent(this, element)) {
            return builder.append(INNER_CLASS_SEPARATOR).append(i + 1)
        }
    }

    throw IllegalStateException("Failed to determine anonymous class for $this")
}



private val PsiElement.anonymousElements: Array<PsiElement>?
    get() {
        for (provider in Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {
            val elements = provider.getAnonymousElements(this)
            if (elements.isNotEmpty()) {
                return elements
            }
        }

        return null
    }
