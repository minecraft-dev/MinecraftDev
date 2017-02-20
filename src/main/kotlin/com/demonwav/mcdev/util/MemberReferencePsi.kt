/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.util.containers.stream
import org.jetbrains.annotations.Contract
import java.util.stream.Stream

// Class

@Contract(pure = true)
fun PsiClass.findMethods(member: MemberReference, checkBases: Boolean = false): Stream<PsiMethod> {
    if (!member.matchOwner(this)) {
        return Stream.empty()
    }

    val result = findMethodsByInternalName(member.name, checkBases)
    return if (member.descriptor != null) {
        result.stream().filter { it.descriptor == member.descriptor }
    } else {
        result.stream()
    }
}

@Contract(pure = true)
fun PsiClass.findField(member: MemberReference, checkBases: Boolean = false): PsiField? {
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

@get:Contract(pure = true)
val PsiMethod.memberReference
    get() = MemberReference(internalName, descriptor)

@get:Contract(pure = true)
val PsiMethod.qualifiedMemberReference
    get() = MemberReference(internalName, descriptor, containingClass!!.fullQualifiedName)

@Contract(pure = true)
fun PsiMethod.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return MemberReference(internalName, descriptor, owner.fullQualifiedName)
}


// Field

@get:Contract(pure = true)
val PsiField.simpleMemberReference
    get() = MemberReference(name!!)

@get:Contract(pure = true)
val PsiField.memberReference
    get() = MemberReference(name!!, descriptor)

@get:Contract(pure = true)
val PsiField.simpleQualifiedMemberReference
    get() = MemberReference(name!!, null, containingClass!!.fullQualifiedName)

@get:Contract(pure = true)
val PsiField.qualifiedMemberReference
    get() = MemberReference(name!!, descriptor, containingClass!!.fullQualifiedName)

@Contract(pure = true)
fun PsiField.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return MemberReference(name!!, descriptor, owner.fullQualifiedName)
}
