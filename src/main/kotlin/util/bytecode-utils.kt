/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
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
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.plugins.groovy.lang.resolve.processors.inference.type

private const val INTERNAL_CONSTRUCTOR_NAME = "<init>"

// Type

val PsiPrimitiveType.internalName: Char
    get() = when (this) {
        PsiTypes.byteType() -> 'B'
        PsiTypes.charType() -> 'C'
        PsiTypes.doubleType() -> 'D'
        PsiTypes.floatType() -> 'F'
        PsiTypes.intType() -> 'I'
        PsiTypes.longType() -> 'J'
        PsiTypes.shortType() -> 'S'
        PsiTypes.booleanType() -> 'Z'
        PsiTypes.voidType() -> 'V'
        else -> throw IllegalArgumentException("Unsupported primitive type: $this")
    }

fun getPrimitiveType(internalName: Char): PsiPrimitiveType? {
    return when (internalName) {
        'B' -> PsiTypes.byteType()
        'C' -> PsiTypes.charType()
        'D' -> PsiTypes.doubleType()
        'F' -> PsiTypes.floatType()
        'I' -> PsiTypes.intType()
        'J' -> PsiTypes.longType()
        'S' -> PsiTypes.shortType()
        'Z' -> PsiTypes.booleanType()
        'V' -> PsiTypes.voidType()
        else -> null
    }
}

val PsiType.descriptor
    get() = appendDescriptor(StringBuilder()).toString()

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

val PsiClass.internalName: String?
    get() {
        realName?.let { return it }
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

val PsiMethod.internalName: String
    get() {
        val realName = realName
        return when {
            isConstructor -> INTERNAL_CONSTRUCTOR_NAME
            realName != null -> realName
            else -> name
        }
    }

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
    if (isConstructor) {
        containingClass?.let { containingClass ->
            if (containingClass.hasModifierProperty(PsiModifier.STATIC)) return@let
            val outerClass = containingClass.containingClass
            outerClass?.type()?.appendDescriptor(builder)
        }
    }
    for (parameter in parameterList.parameters) {
        parameter.type.appendDescriptor(builder)
    }
    builder.append(')')
    return (returnType ?: PsiTypes.voidType()).appendDescriptor(builder)
}

// Field
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
