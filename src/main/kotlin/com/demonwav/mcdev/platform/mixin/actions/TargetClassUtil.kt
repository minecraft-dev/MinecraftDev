/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.demonwav.mcdev.util.nameAndDescriptor
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiSubstitutor
import com.intellij.util.containers.isEmpty
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import java.util.stream.Stream

internal fun findMethods(psiClass: PsiClass): Stream<PsiMethod>? {
    val targets = MixinUtils.getAllMixedClasses(psiClass).values
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

internal fun findFields(psiClass: PsiClass): Stream<PsiField>? {
    val targets = MixinUtils.getAllMixedClasses(psiClass).values
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

internal fun copyMember(project: Project, psiClass: PsiClass, member: PsiMember): PsiMember {
    return when (member) {
        is PsiMethod -> copyMethod(project, psiClass, member)
        is PsiField -> copyField(project, member)
        else -> throw UnsupportedOperationException("Unsupported member type: ${member.javaClass.name}")
    }
}

internal fun copyMethod(project: Project, psiClass: PsiClass, method: PsiMethod): PsiMethod {
    val newMethod = GenerateMembersUtil.substituteGenericMethod(method, PsiSubstitutor.EMPTY, psiClass)
    OverrideImplementUtil.deleteDocComment(newMethod)

    // Copy modifiers
    copyModifiers(method.modifierList, newMethod.modifierList)

    // Copy annotations
    val factory = JavaPsiFacade.getElementFactory(project)
    for (annotation in method.modifierList.annotations) {
        newMethod.modifierList.addAfter(factory.createAnnotationFromText(annotation.text, method), null)
    }

    return newMethod
}

private fun copyField(project: Project, field: PsiField): PsiField {
    val factory = JavaPsiFacade.getElementFactory(project)

    val fieldModifiers = field.modifierList!!
    val newField = factory.createField(field.name!!, field.type)
    val newModifiers = newField.modifierList!!

    // Copy annotations
    for (annotation in fieldModifiers.annotations) {
        newModifiers.addAfter(factory.createAnnotationFromText(annotation.text, field), null)
    }

    copyModifiers(fieldModifiers, newModifiers)

    return newField
}

private fun copyModifiers(modifiers: PsiModifierList, newModifiers: PsiModifierList) {
    for (modifier in PsiModifier.MODIFIERS) {
        if (modifiers.hasExplicitModifier(modifier)) {
            newModifiers.setModifierProperty(modifier, true)
        }
    }
}
