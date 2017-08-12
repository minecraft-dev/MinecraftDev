/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.sided

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findMethodsByInternalName
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.parseClassDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

fun parseInternalIdentifier(identifier: String, project: Project, scope: GlobalSearchScope): PsiElement? {
    val index = identifier.indexOf('(')
    if (index != -1) {
        // method
        val classIndex = identifier.indexOf(';')
        val classDescriptor = identifier.substring(0, classIndex)
        val descriptor = identifier.substring(index)
        return findQualifiedClass(project, parseClassDescriptor(classDescriptor), scope)
            ?.findMethodsByInternalName(identifier.substring(classIndex + 1, index))
            ?.asSequence()
            ?.firstOrNull { it.descriptor == descriptor }
    } else if (identifier.endsWith(';')) {
        // class
        return findQualifiedClass(project, parseClassDescriptor(identifier), scope)
    } else {
        // field
        val classIndex = identifier.indexOf(';')
        val classDescriptor = identifier.substring(0, classIndex)
        return findQualifiedClass(project, parseClassDescriptor(classDescriptor), scope)
            ?.findFieldByName(identifier.substring(classIndex + 1), false)
    }
}

fun toInternalIdentifier(element: PsiElement?): String? {
    return when (element) {
        is PsiClass -> element.descriptor
        is PsiMethod -> (element.containingClass?.descriptor ?: return null) + element.name + element.descriptor
        is PsiField -> (element.containingClass?.descriptor ?: return null) + element.name
        else -> null
    }
}

fun getInferenceReason(state: SideState, name: String?, project: Project): String? {
    if (state.reasonPointer != null) {
        val reason = state.computeReason(project)?.name ?: "null"
        return state.reason?.getText(name ?: "null", state.side.annotation, reason)
    } else {
        return null
    }
}
