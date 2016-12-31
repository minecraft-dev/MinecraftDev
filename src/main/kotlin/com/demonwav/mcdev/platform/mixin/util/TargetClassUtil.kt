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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsMethodImpl
import com.intellij.util.containers.isEmpty
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

internal fun findMethods(psiClass: PsiClass, targets: Collection<PsiClass>, checkBases: Boolean = false): Stream<PsiMethod>? {
    return when (targets.size) {
        0 -> null
        1 -> targets.single().methods.stream()
                .filter({!it.isConstructor})
        else -> targets.stream()
                .flatMap { target -> target.methods.stream() }
                .filter({!it.isConstructor})
                .collect(Collectors.groupingBy(PsiMethod::memberReference))
                .values.stream()
                .filter { it.size >= targets.size }
                .map { it.first() }
    }?.filter {
        // Filter methods which are already in the Mixin class
        psiClass.findMethods(it.memberReference, checkBases).isEmpty()
    }
}

internal fun findFields(psiClass: PsiClass, targets: Collection<PsiClass>, checkBases: Boolean = false): Stream<PsiField>? {
    return when (targets.size) {
        0 -> null
        1 -> targets.single().fields.stream()
        else -> targets.stream()
                .flatMap { target -> target.fields.stream() }
                .collect(Collectors.groupingBy(PsiField::memberReference))
                .values.stream()
                .filter { it.size >= targets.size }
                .map { it.first() }
    }?.filter {
        // Filter fields which are already in the Mixin class
        psiClass.findFieldByName(it.name, checkBases) == null
    }
}

internal fun PsiMethod.findSource(): PsiMethod {
    val body = body
    if (body != null) {
        return this
    }

    // Attempt to find the source if we have a compiled method
    return (this as? ClsMethodImpl)?.sourceMirrorMethod ?: this
}
