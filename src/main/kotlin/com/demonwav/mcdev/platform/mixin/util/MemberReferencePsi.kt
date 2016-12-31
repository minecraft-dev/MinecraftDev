/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findMethodsByInternalName
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.internalName
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.util.containers.stream
import java.util.stream.Stream

// Class

internal fun PsiClass.findMethods(member: MemberReference, checkBases: Boolean = false): Stream<PsiMethod> {
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

internal fun PsiClass.findField(member: MemberReference, checkBases: Boolean = false): PsiField? {
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

internal val PsiMethod.memberReference
    get() = MemberReference(internalName, descriptor)

internal val PsiMethod.qualifiedMemberReference
    get() = MemberReference(internalName, descriptor, containingClass!!.fullQualifiedName)

internal fun PsiMethod.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return MemberReference(internalName, descriptor, owner.fullQualifiedName)
}


// Field

internal val PsiField.memberReference
    get() = MemberReference(name!!, descriptor)

internal val PsiField.qualifiedMemberReference
    get() = MemberReference(name!!, descriptor, containingClass!!.fullQualifiedName)

internal fun PsiField.getQualifiedMemberReference(owner: PsiClass): MemberReference {
    return MemberReference(name!!, descriptor, owner.fullQualifiedName)
}
