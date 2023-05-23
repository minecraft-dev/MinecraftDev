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

package com.demonwav.mcdev.platform.mcp.srg

import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.google.common.collect.ImmutableBiMap
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class McpSrgMap(
    val classMap: ImmutableBiMap<String, String>,
    val fieldMap: ImmutableBiMap<MemberReference, MemberReference>,
    val methodMap: ImmutableBiMap<MemberReference, MemberReference>,
    private val srgNames: HashMap<String, String>,
) {
    fun getSrgClass(fullQualifiedName: String) = classMap[fullQualifiedName]
    fun getSrgClass(psiClass: PsiClass): String? {
        return getSrgClass(psiClass.fullQualifiedName ?: return null)
    }

    fun getSrgField(reference: MemberReference) = fieldMap[reference]
    fun getSrgField(field: PsiField) = getSrgField(field.simpleQualifiedMemberReference)

    fun getSrgMethod(reference: MemberReference) = methodMap[reference]
    fun getSrgMethod(method: PsiMethod) = getSrgMethod(method.qualifiedMemberReference)

    fun getMcpClass(fullQualifiedName: String) = classMap.inverse()[fullQualifiedName]
    fun mapToMcpClass(fullQualifiedName: String) = getMcpClass(fullQualifiedName) ?: fullQualifiedName

    fun getMcpField(reference: MemberReference) = fieldMap.inverse()[reference]
    fun mapToMcpField(reference: MemberReference) = getMcpField(reference) ?: reference
    fun findMcpField(field: PsiField) = getMcpField(field.qualifiedMemberReference)

    fun getMcpMethod(reference: MemberReference) = methodMap.inverse()[reference]
    fun mapToMcpMethod(reference: MemberReference) = getMcpMethod(reference) ?: reference
    fun findMcpMethod(method: PsiMethod) = getMcpMethod(method.qualifiedMemberReference)

    fun mapMcpToSrgName(name: String) = srgNames[name]
}
