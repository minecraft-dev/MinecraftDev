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
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement

// Type

val PsiClassType.fullQualifiedName
    get() = resolve()!!.fullQualifiedName

// Class

val PsiClass.fullQualifiedName: String
    get() {
        val parentClass = containingClass ?: return qualifiedName!!
        return buildFullQualifiedName(StringBuilder(), parentClass).toString()
    }

fun PsiClass.appendFullQualifiedName(builder: StringBuilder): StringBuilder {
    val parentClass = containingClass ?: return builder.append(qualifiedName!!)
    return buildFullQualifiedName(builder, parentClass)
}

private fun PsiClass.buildFullQualifiedName(builder: StringBuilder, parentClass: PsiClass): StringBuilder {
    buildInnerName(builder, parentClass, { builder.append(it.qualifiedName!!) })
    return builder
}

val PsiClass.shortName: String
    get() {
        val parentClass = containingClass ?: return name!!
        val builder = StringBuilder()
        buildInnerName(builder, parentClass, { builder.append(it.name!!) }, '.')
        return builder.toString()
    }

inline fun PsiClass.buildInnerName(builder: StringBuilder, firstParentClass: PsiClass,
                                   outer: (PsiClass) -> Unit, separator: Char = '$') {
    var parentClass: PsiClass? = firstParentClass
    var currentClass: PsiClass = this
    val list = ArrayList<String>()

    while (parentClass != null) {
        val name = currentClass.name
        if (name != null) {
            // Named inner class
            list.add(name)
        } else {
            // Attempt to find name for anonymous class
            for ((i, element) in currentClass.anonymousElements!!.withIndex()) {
                if (currentClass.manager.areElementsEquivalent(this, element)) {
                    list.add((i + 1).toString())
                    break
                }
            }

            throw IllegalStateException("Failed to determine anonymous class for $currentClass")
        }

        currentClass = parentClass
        parentClass = currentClass.containingClass
    }

    outer(currentClass)

    for (i in list.lastIndex downTo 0) {
        builder.append(separator).append(list[i])
    }
}

val PsiElement.anonymousElements: Array<PsiElement>?
    get() {
        for (provider in Extensions.getExtensions(AnonymousElementProvider.EP_NAME)) {
            val elements = provider.getAnonymousElements(this)
            if (elements.isNotEmpty()) {
                return elements
            }
        }

        return null
    }
