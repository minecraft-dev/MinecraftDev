/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.demonwav.mcdev.util.nameAndDescriptor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.util.containers.isEmpty
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

internal fun findMethods(psiClass: PsiClass, targets: Collection<PsiClass>): Stream<PsiMethod>? {
    return when (targets.size) {
        0 -> null
        1 -> targets.single().methods.stream()
                .filter({!it.isConstructor})
        else -> targets.stream()
                .flatMap { target -> target.methods.stream() }
                .filter({!it.isConstructor})
                .collect(Collectors.groupingBy(PsiMethod::internalNameAndDescriptor))
                .values.stream()
                .filter { it.size >= targets.size }
                .map { it.first() }
    }?.filter {
        // Filter methods which are already in the Mixin class
        psiClass.findMethodsByInternalNameAndDescriptor(it.internalNameAndDescriptor).isEmpty()
    }
}

internal fun findFields(psiClass: PsiClass, targets: Collection<PsiClass>): Stream<PsiField>? {
    return when (targets.size) {
        0 -> null
        1 -> targets.single().fields.stream()
        else -> targets.stream()
                .flatMap { target -> target.fields.stream() }
                .collect(Collectors.groupingBy(PsiField::nameAndDescriptor))
                .values.stream()
                .filter { it.size >= targets.size }
                .map { it.first() }
    }?.filter {
        // Filter fields which are already in the Mixin class
        psiClass.findFieldByName(it.name, false) == null
    }
}
