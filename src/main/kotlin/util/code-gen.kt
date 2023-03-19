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

import com.intellij.codeInsight.generation.PsiGenerationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope

fun createVoidMethodWithParameterType(project: Project, name: String, paramType: PsiClass): PsiMethod? {
    val newMethod = JavaPsiFacade.getElementFactory(project).createMethod(name, PsiTypes.voidType())

    val list = newMethod.parameterList
    val qName = paramType.qualifiedName ?: return null
    val parameter = JavaPsiFacade.getElementFactory(project)
        .createParameter(
            "event",
            PsiClassType.getTypeByName(qName, project, GlobalSearchScope.allScope(project)),
        )
    list.add(parameter)

    return newMethod
}

fun <T : PsiMember> generationInfoFromMethod(method: T, annotation: PsiAnnotation, newMethod: T): PsiGenerationInfo<T> {
    val realName = method.realName
    if (realName != null && realName != method.name) {
        val elementFactory = JavaPsiFacade.getElementFactory(method.project)
        val value = elementFactory.createExpressionFromText(
            "\"${StringUtil.escapeStringCharacters(realName)}\"",
            annotation,
        )
        annotation.setDeclaredAttributeValue("aliases", value)
    }
    return PsiGenerationInfo(newMethod)
}
