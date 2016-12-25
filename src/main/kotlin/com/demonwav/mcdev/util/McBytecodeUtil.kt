/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("McBytecodeUtil")
package com.demonwav.mcdev.util

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.util.containers.stream
import java.util.stream.Stream

private val INTERNAL_CONSTRUCTOR_NAME = "<init>"

// Type

val PsiPrimitiveType.internalName: String?
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
        else -> null
    }

val PsiClassType.internalName: String
    get() = if (parameterCount == 0) canonicalText.replace('.', '/') else rawType().internalName

val PsiType.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiType.appendDescriptor(builder: StringBuilder): StringBuilder {
    return when (this) {
        is PsiPrimitiveType -> builder.append(internalName!!)
        is PsiArrayType -> componentType.appendDescriptor(builder.append('['))
        is PsiClassType -> builder.append('L').append(internalName).append(';')
        else -> throw IllegalArgumentException("Unsupported PsiType: $this")
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

fun PsiClass.findMethodsByInternalName(internalName: String, checkBases: Boolean = false): Stream<PsiMethod> {
    return (if (internalName == INTERNAL_CONSTRUCTOR_NAME) {
        constructors
    } else {
        findMethodsByName(internalName, checkBases)
    }).stream()
}

fun PsiClass.findMethodsByInternalNameAndDescriptor(inad: String, checkBases: Boolean = false): Stream<PsiMethod> {
    val pos = inad.indexOf('(')

    return if (pos >= 0) {
        val descriptor = inad.substring(pos)
        findMethodsByInternalName(inad.substring(0, pos), checkBases)
                .filter { descriptor == it.descriptor }
    } else {
        findMethodsByInternalName(inad, checkBases)
    }
}

fun PsiClass.findFieldByNameAndDescriptor(nad: String, checkBases: Boolean = false): PsiField? {
    val pos = nad.indexOf(':')
    return if (pos >= 0) {
        val field = findFieldByName(nad.substring(0, pos), checkBases)
        if (field?.descriptor == nad.substring(pos)) field else null
    } else {
        findFieldByName(nad, checkBases)
    }
}

// Method

val PsiMethod.internalName: String
    get() = if (isConstructor) INTERNAL_CONSTRUCTOR_NAME else name


val PsiMethod.internalNameAndDescriptor: String
    get() = appendDescriptor(StringBuilder(internalName)).toString()

val PsiMethod.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiMethod.appendDescriptor(builder: StringBuilder): StringBuilder {
    builder.append('(')
    for (parameter in parameterList.parameters) {
        parameter.typeElement?.type?.appendDescriptor(builder)
    }
    builder.append(')')
    return (returnType ?: PsiType.VOID).appendDescriptor(builder)
}

// Field

val PsiField.nameAndDescriptor: String
    get() = appendDescriptor(StringBuilder(name).append(':')).toString()

val PsiField.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiField.appendDescriptor(builder: StringBuilder): StringBuilder = type.appendDescriptor(builder)
