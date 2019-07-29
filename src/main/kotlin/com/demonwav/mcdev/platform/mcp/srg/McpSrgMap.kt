/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
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
    private val srgNames: HashMap<String, String>
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
