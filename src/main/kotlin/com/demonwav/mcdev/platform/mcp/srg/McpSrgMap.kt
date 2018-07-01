/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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
import org.jetbrains.annotations.Contract
import java.nio.file.Files
import java.nio.file.Path

class McpSrgMap private constructor(
    val classMap: ImmutableBiMap<String, String>,
    val fieldMap: ImmutableBiMap<MemberReference, MemberReference>,
    val methodMap: ImmutableBiMap<MemberReference, MemberReference>,
    private val srgNames: Map<String, String>
) {

    @Contract(pure = true) fun getSrgClass(fullQualifiedName: String) = classMap[fullQualifiedName]
    @Contract(pure = true) fun mapToSrgClass(fullQualifiedName: String) = getSrgClass(fullQualifiedName) ?: fullQualifiedName
    @Contract(pure = true) fun findSrgClass(psiClass: PsiClass): String? {
        return getSrgClass(psiClass.fullQualifiedName ?: return null)
    }

    @Contract(pure = true) fun getSrgField(reference: MemberReference) = fieldMap[reference]
    @Contract(pure = true) fun mapToSrgField(reference: MemberReference) = getSrgField(reference) ?: reference
    @Contract(pure = true) fun findSrgField(field: PsiField) = getSrgField(field.simpleQualifiedMemberReference)

    @Contract(pure = true) fun getSrgMethod(reference: MemberReference) = methodMap[reference]
    @Contract(pure = true) fun mapToSrgMethod(reference: MemberReference) = methodMap[reference] ?: reference
    @Contract(pure = true) fun findSrgMethod(method: PsiMethod) = getSrgMethod(method.qualifiedMemberReference)

    @Contract(pure = true) fun getMcpClass(fullQualifiedName: String) = classMap.inverse()[fullQualifiedName]
    @Contract(pure = true) fun mapToMcpClass(fullQualifiedName: String) = getMcpClass(fullQualifiedName) ?: fullQualifiedName

    @Contract(pure = true) fun getMcpField(reference: MemberReference) = fieldMap.inverse()[reference]
    @Contract(pure = true) fun mapToMcpField(reference: MemberReference) = getMcpField(reference) ?: reference
    @Contract(pure = true) fun findMcpField(field: PsiField) = getMcpField(field.qualifiedMemberReference)

    @Contract(pure = true) fun getMcpMethod(reference: MemberReference) = methodMap.inverse()[reference]
    @Contract(pure = true) fun mapToMcpMethod(reference: MemberReference) = getMcpMethod(reference) ?: reference
    @Contract(pure = true) fun findMcpMethod(method: PsiMethod) = getMcpMethod(method.qualifiedMemberReference)

    @Contract(pure = true) fun mapSrgName(name: String) = srgNames[name]

    companion object {

        fun parse(path: Path): McpSrgMap {
            val classMapBuilder = ImmutableBiMap.builder<String, String>()
            val fieldMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
            val methodMapBuilder = ImmutableBiMap.builder<MemberReference, MemberReference>()
            val srgNames = hashMapOf<String, String>()

            for (line in Files.readAllLines(path)) {
                val parts = line.split(' ')
                when (parts[0]) {
                    "CL:" -> classMapBuilder.put(parts[1].replace('/', '.'), parts[2].replace('/', '.'))
                    "FD:" -> {
                        val mcp = SrgMemberReference.parse(parts[1])
                        val srg = SrgMemberReference.parse(parts[2])
                        fieldMapBuilder.put(mcp, srg)
                        srgNames[srg.name] = mcp.name
                    }
                    "MD:" -> {
                        val mcp = SrgMemberReference.parse(parts[1], parts[2])
                        val srg = SrgMemberReference.parse(parts[3], parts[4])
                        methodMapBuilder.put(mcp, srg)
                        srgNames[srg.name] =  mcp.name
                    }
                }
            }

            return McpSrgMap(classMapBuilder.build(), fieldMapBuilder.build(), methodMapBuilder.build(), srgNames)
        }
    }
}
