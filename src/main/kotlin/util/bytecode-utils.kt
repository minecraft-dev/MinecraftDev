/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDisjunctionType
import com.intellij.psi.PsiIntersectionType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiWildcardType
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
    get() = erasure().appendDescriptor(StringBuilder()).toString()

private fun PsiType.erasure() = TypeConversionUtil.erasure(this)!!

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
        is PsiWildcardType -> extendsBound.appendDescriptor(builder)
        is PsiIntersectionType -> conjuncts.first().appendDescriptor(builder)
        is PsiDisjunctionType -> leastUpperBound.appendDescriptor(builder)
        else -> throw IllegalArgumentException("Unsupported PsiType: $this")
    }
}

val PsiType.signature
    get() = appendSignature(StringBuilder()).toString()

private fun PsiType.appendSignature(builder: StringBuilder): StringBuilder {
    return when (this) {
        is PsiPrimitiveType -> builder.append(internalName)
        is PsiArrayType -> componentType.appendSignature(builder.append('['))
        is PsiClassType -> {
            val resolveResult = resolveGenerics()
            val resolved = resolveResult.element ?: return builder
            val substitutions = resolveResult.substitutor.substitutionMap
            if (resolved is PsiTypeParameter) {
                builder.append('T').append(resolved.name).append(';')
            } else {
                builder.append('L')
                val classes = generateSequence(resolved) { it.containingClass }.toList()
                var firstClass = true
                var hadGenerics = false
                for (clazz in classes.asReversed()) {
                    if (firstClass) {
                        clazz.appendInternalName(builder)
                        firstClass = false
                    } else {
                        if (hadGenerics) {
                            builder.append('.')
                        } else {
                            builder.append('$')
                        }
                        builder.append(clazz.name)
                    }

                    val typeArgs = clazz.typeParameterList?.typeParameters?.map(substitutions::get)
                        ?: emptyList()
                    if (typeArgs.isNotEmpty() && typeArgs.all { it != null }) {
                        hadGenerics = true
                        builder.append('<')
                        for (typeArg in typeArgs) {
                            typeArg!!.appendSignature(builder)
                        }
                        builder.append('>')
                    }
                }
                builder.append(';')
            }
        }
        is PsiIntersectionType -> conjuncts.first().appendSignature(builder)
        is PsiDisjunctionType -> leastUpperBound.appendSignature(builder)
        is PsiWildcardType -> when {
            isExtends -> extendsBound.appendSignature(builder.append('+'))
            isSuper -> superBound.appendSignature(builder.append('-'))
            else -> builder.append('*')
        }
        else -> throw IllegalArgumentException("Unsupported PsiType: $this")
    }
}

private fun PsiTypeParameter.appendSignature(builder: StringBuilder): StringBuilder {
    builder.append(name)

    val extendsList = this.extendsList.referencedTypes
    if (extendsList.isEmpty()) {
        return builder.append(":Ljava/lang/Object;")
    }

    val classBound = extendsList.first().takeIf { classBound ->
        classBound.resolve()?.isInterface != true
    }

    builder.append(':')
    classBound?.appendSignature(builder)

    for (interfaceBound in extendsList.drop(if (classBound != null) 1 else 0)) {
        interfaceBound.appendSignature(builder.append(':'))
    }

    return builder
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

val PsiClass.signature: String
    get() {
        val builder = StringBuilder()

        val typeParams = typeParameterList?.typeParameters
        if (!typeParams.isNullOrEmpty()) {
            builder.append('<')
            for (typeParam in typeParams) {
                typeParam.appendSignature(builder)
            }
            builder.append('>')
        }

        val superType = this.extendsListTypes.singleOrNull()
        if (superType == null || isInterface) {
            builder.append("Ljava/lang/Object;")
        } else {
            superType.appendSignature(builder)
        }
        val interfaces = if (isInterface) this.extendsListTypes else this.implementsListTypes
        for (itf in interfaces) {
            itf.appendSignature(builder)
        }
        return builder.toString()
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

val PsiMethod.signature: String
    get() {
        val builder = StringBuilder()

        val typeParams = typeParameterList?.typeParameters
        if (!typeParams.isNullOrEmpty()) {
            builder.append('<')
            for (typeParam in typeParams) {
                typeParam.appendSignature(builder)
            }
            builder.append('>')
        }

        builder.append('(')
        for (parameter in parameterList.parameters) {
            parameter.type.appendSignature(builder)
        }
        builder.append(')')
        returnType?.appendSignature(builder)

        for (exception in throwsList.referencedTypes) {
            exception.appendSignature(builder.append('^'))
        }

        return builder.toString()
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
val PsiVariable.descriptor: String?
    get() {
        return try {
            appendDescriptor(StringBuilder()).toString()
        } catch (e: ClassNameResolutionFailedException) {
            null
        }
    }

val PsiVariable.signature: String
    get() = type.signature

@Throws(ClassNameResolutionFailedException::class)
private fun PsiVariable.appendDescriptor(builder: StringBuilder): StringBuilder = type.appendDescriptor(builder)
