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
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.containers.stream
import java.util.stream.Stream

private const val INTERNAL_CONSTRUCTOR_NAME = "<init>"

// Type

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

private fun PsiClassType.erasure() = TypeConversionUtil.erasure(this) as PsiClassType

val PsiClassType.internalName
    get() = erasure().resolve()!!.internalName

fun PsiClassType.appendInternalName(builder: StringBuilder): StringBuilder = erasure().resolve()!!.appendInternalName(builder)

val PsiType.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiType.appendDescriptor(builder: StringBuilder): StringBuilder {
    return when (this) {
        is PsiPrimitiveType -> builder.append(internalName)
        is PsiArrayType -> componentType.appendDescriptor(builder.append('['))
        is PsiClassType -> appendInternalName(builder.append('L')).append(';')
        else -> throw IllegalArgumentException("Unsupported PsiType: $this")
    }
}

// Class

val PsiClass.internalName: String
    get() {
        val parentClass = containingClass ?: return qualifiedName!!.replace('.', '/')
        return buildInternalName(StringBuilder(), parentClass).toString()
    }

fun PsiClass.appendInternalName(builder: StringBuilder): StringBuilder {
    val parentClass = containingClass ?: return builder.append(qualifiedName!!.replace('.', '/'))
    return buildInternalName(builder, parentClass)
}

private fun PsiClass.buildInternalName(builder: StringBuilder, parentClass: PsiClass): StringBuilder {
    buildInnerName(builder, parentClass, { builder.append(it.qualifiedName!!.replace('.', '/')) })
    return builder
}

val PsiClass.descriptor: String?
    get() = appendInternalName(StringBuilder().append('L')).append(';').toString()

fun PsiClass.appendDescriptor(builder: StringBuilder): StringBuilder = appendInternalName(builder.append('L')).append(';')

fun PsiClass.findMethodsByInternalName(internalName: String, checkBases: Boolean = false): Stream<PsiMethod> {
    return (if (internalName == INTERNAL_CONSTRUCTOR_NAME) {
        constructors
    } else {
        findMethodsByName(internalName, checkBases)
    }).stream()
}

fun PsiClass.findMethods(member: MemberDescriptor, checkBases: Boolean = false): Stream<PsiMethod> {
    if (!member.matchOwner(this)) {
        return Stream.empty()
    }

    val result = findMethodsByInternalName(member.name, checkBases)
    return if (member.descriptor != null) {
        result.filter { it.descriptor == member.descriptor }
    } else {
        result
    }
}

fun PsiClass.findField(member: MemberDescriptor, checkBases: Boolean = false): PsiField? {
    if (!member.matchOwner(this)) {
        return null
    }

    val field = findFieldByName(member.name, checkBases) ?: return null
    if (member.descriptor != null && member.descriptor != field.descriptor) {
        return null
    }

    return field
}

// Method

val PsiMethod.internalName: String
    get() = if (isConstructor) INTERNAL_CONSTRUCTOR_NAME else name

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

val PsiMethod.memberDescriptor
    get() = MemberDescriptor(internalName, descriptor)

val PsiMethod.qualifiedMemberDescriptor
    get() = MemberDescriptor(internalName, descriptor, containingClass!!.fullQualifiedName)

fun PsiMethod.getQualifiedMemberDescriptor(qualifier: PsiClassType?): MemberDescriptor {
    qualifier ?: return qualifiedMemberDescriptor
    return MemberDescriptor(internalName, descriptor, qualifier.fullQualifiedName)
}

// Field
val PsiField.descriptor: String
    get() = appendDescriptor(StringBuilder()).toString()

fun PsiField.appendDescriptor(builder: StringBuilder): StringBuilder = type.appendDescriptor(builder)

val PsiField.memberDescriptor
    get() = MemberDescriptor(name!!, descriptor)

val PsiField.qualifiedMemberDescriptor
    get() = MemberDescriptor(name!!, descriptor, containingClass!!.fullQualifiedName)

fun PsiField.getQualifiedMemberDescriptor(qualifier: PsiClassType?): MemberDescriptor {
    qualifier ?: return qualifiedMemberDescriptor
    return MemberDescriptor(name!!, descriptor, qualifier.fullQualifiedName)
}
