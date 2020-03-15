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

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.Contract

private const val INTERNAL_CONSTRUCTOR_NAME = "<init>"

// Type

@get:Contract(pure = true)
val PsiPrimitiveType.internalName: Char
    get() = when (this) {
        PsiType.BYTE -> 'B'
        PsiType.CHAR -> 'C'
        PsiType.DOUBLE -> 'D'
        PsiType.FLOAT -> 'F'
        PsiType.INT -> 'I'
        PsiType.LONG -> 'J'
        PsiType.SHORT -> 'S'
        PsiType.BOOLEAN -> 'Z'
        PsiType.VOID -> 'V'
        else -> throw IllegalArgumentException("Unsupported primitive type: $this")
    }

fun getPrimitiveType(internalName: Char): PsiPrimitiveType? {
    return when (internalName) {
        'B' -> PsiType.BYTE
        'C' -> PsiType.CHAR
        'D' -> PsiType.DOUBLE
        'F' -> PsiType.FLOAT
        'I' -> PsiType.INT
        'J' -> PsiType.LONG
        'S' -> PsiType.SHORT
        'Z' -> PsiType.BOOLEAN
        'V' -> PsiType.VOID
        else -> null
    }
}

fun getPrimitiveWrapperClass(internalName: Char, project: Project): PsiClass? {
    val type = getPrimitiveType(internalName) ?: return null
    val boxedTypeName = type.boxedTypeName ?: return null
    return JavaPsiFacade.getInstance(project).findClass(boxedTypeName, GlobalSearchScope.allScope(project))
}

private fun PsiClassType.erasure() = TypeConversionUtil.erasure(this) as PsiClassType

@Throws(ClassNameResolutionFailedException::class)
private fun PsiClassType.appendInternalName(builder: StringBuilder): StringBuilder =
    erasure().resolve()?.appendInternalName(builder) ?: builder

@Throws(ClassNameResolutionFailedException::class)
private fun PsiType.appendDescriptor(builder: StringBuilder): StringBuilder {
    return when (this) {
        is PsiPrimitiveType -> builder.append(internalName)
        is PsiArrayType -> componentType.appendDescriptor(builder.append('['))
        is PsiClassType -> appendInternalName(builder.append('L')).append(';')
        else -> throw IllegalArgumentException("Unsupported PsiType: $this")
    }
}

fun parseClassDescriptor(descriptor: String): String {
    val internalName = descriptor.substring(1, descriptor.length - 1)
    return internalName.replace('/', '.')
}

// Class

@get:Contract(pure = true)
val PsiClass.internalName: String?
    get() {
        return try {
            outerQualifiedName?.replace('.', '/') ?: buildInternalName(StringBuilder()).toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

@Throws(ClassNameResolutionFailedException::class)
private fun PsiClass.appendInternalName(builder: StringBuilder): StringBuilder {
    return outerQualifiedName?.let { builder.append(it.replace('.', '/')) } ?: buildInternalName(builder)
}

@Throws(ClassNameResolutionFailedException::class)
private fun PsiClass.buildInternalName(builder: StringBuilder): StringBuilder {
    buildInnerName(builder, { it.outerQualifiedName?.replace('.', '/') })
    return builder
}

@get:Contract(pure = true)
val PsiClass.descriptor: String?
    get() {
        return try {
            appendInternalName(StringBuilder().append('L')).append(';').toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

fun PsiClass.findMethodsByInternalName(internalName: String, checkBases: Boolean = false): Array<PsiMethod> {
    return if (internalName == INTERNAL_CONSTRUCTOR_NAME) {
        constructors
    } else {
        findMethodsByName(internalName, checkBases)
    }
}

// Method

@get:Contract(pure = true)
val PsiMethod.internalName: String
    get() = if (isConstructor) INTERNAL_CONSTRUCTOR_NAME else name

@get:Contract(pure = true)
val PsiMethod.descriptor: String?
    get() {
        return try {
            appendDescriptor(StringBuilder()).toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

@Throws(ClassNameResolutionFailedException::class)
private fun PsiMethod.appendDescriptor(builder: StringBuilder): StringBuilder {
    builder.append('(')
    for (parameter in parameterList.parameters) {
        parameter.typeElement?.type?.appendDescriptor(builder)
    }
    builder.append(')')
    return (returnType ?: PsiType.VOID).appendDescriptor(builder)
}

// Field
@get:Contract(pure = true)
val PsiField.descriptor: String?
    get() {
        return try {
            appendDescriptor(StringBuilder()).toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

@Throws(ClassNameResolutionFailedException::class)
private fun PsiField.appendDescriptor(builder: StringBuilder): StringBuilder = type.appendDescriptor(builder)
