/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
