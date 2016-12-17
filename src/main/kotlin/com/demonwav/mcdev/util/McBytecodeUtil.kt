/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType

// Type

val PsiType.internalName: String
    get() = when (this) {
        PsiType.BYTE -> "B"
        PsiType.CHAR -> "C"
        PsiType.DOUBLE -> "D"
        PsiType.FLOAT -> "F"
        PsiType.INT -> "I"
        PsiType.LONG -> "J"
        PsiType.SHORT -> "S"
        PsiType.BOOLEAN -> "Z"
        PsiType.VOID -> "V"
        else -> canonicalText.replace('.', '/')
    }

val PsiType.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiType.appendDescriptor(builder: StringBuilder): StringBuilder {
    val internalName = internalName
    return if (internalName.length == 1) {
        builder.append(internalName)
    } else {
        builder.append('L').append(internalName).append(';')
    }
}

// Class

val PsiClass.internalName: String?
    get() = qualifiedName?.replace('.', '/')

val PsiClass.descriptor: String?
    get() {
        val internalName = internalName ?: return null
        return "L$internalName;"
    }

fun PsiClass.appendDescriptor(builder: StringBuilder): StringBuilder = builder.append('L').append(internalName!!).append(';')

// Method

val PsiMethod.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiMethod.appendDescriptor(builder: StringBuilder): StringBuilder {
    builder.append('(')
    for (parameter in parameterList.parameters) {
        val type = parameter.typeElement?.type ?: continue
        type.appendDescriptor(builder)
    }
    builder.append(')')
    return (returnType ?: PsiType.VOID).appendDescriptor(builder)
}

// Field

val PsiField.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiField.appendDescriptor(builder: StringBuilder): StringBuilder = type.appendDescriptor(builder)
