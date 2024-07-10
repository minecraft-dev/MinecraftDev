/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.mappings

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.google.common.collect.ImmutableBiMap
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

class Mappings(
    val classMap: ImmutableBiMap<String, String>,
    val fieldMap: ImmutableBiMap<MemberReference, MemberReference>,
    val methodMap: ImmutableBiMap<MemberReference, MemberReference>,
    private val intermediaryNames: HashMap<String, String>,
    val containsFieldDescriptors: Boolean,
) {
    fun getIntermediaryClass(fullQualifiedName: String) = classMap[fullQualifiedName]
    fun getIntermediaryClass(psiClass: PsiClass): String? {
        return getIntermediaryClass(psiClass.fullQualifiedName ?: return null)
    }

    fun getIntermediaryField(reference: MemberReference) = fieldMap[reference]
    fun getIntermediaryField(field: PsiField) = getIntermediaryField(
        if (containsFieldDescriptors) {
            field.qualifiedMemberReference
        } else {
            field.simpleQualifiedMemberReference
        }
    )

    fun getIntermediaryMethod(reference: MemberReference) = methodMap[reference]
    fun getIntermediaryMethod(method: PsiMethod) = getIntermediaryMethod(method.qualifiedMemberReference)

    fun tryGetMappedClass(fullQualifiedName: String) = classMap.inverse()[fullQualifiedName]
    fun getMappedClass(fullQualifiedName: String) = tryGetMappedClass(fullQualifiedName) ?: fullQualifiedName

    fun tryGetMappedField(reference: MemberReference) = fieldMap.inverse()[reference]
    fun getMappedField(reference: MemberReference) = tryGetMappedField(reference) ?: reference
    fun tryGetMappedField(field: PsiField) = tryGetMappedField(field.qualifiedMemberReference)

    fun tryGetMappedMethod(reference: MemberReference) = methodMap.inverse()[reference]
    fun getMappedMethod(reference: MemberReference) = tryGetMappedMethod(reference) ?: reference
    fun tryGetMappedMethod(method: PsiMethod) = tryGetMappedMethod(method.qualifiedMemberReference)

    fun mapIntermediaryToMapped(name: String) = intermediaryNames[name]
}

private val Module.namedToMojang: Mappings? get() {
    val mcFacet = MinecraftFacet.getInstance(this) ?: return null
    val manager = mcFacet.modules.filterIsInstance<HasCustomNamedMappings>().mapFirstNotNull { it.namedToMojangManager }
        ?: return null
    return manager.mappingsNow?.takeUnless { it.containsFieldDescriptors }
}

fun Module.getMappedClass(mojangName: String): String {
    return namedToMojang?.getMappedClass(mojangName) ?: mojangName
}

fun Module.getMojangClass(mappedName: String): String {
    return namedToMojang?.getIntermediaryClass(mappedName) ?: mappedName
}

fun Module.getMojangClass(clazz: PsiClass): String? {
    return getMojangClass(clazz.fullQualifiedName ?: return null)
}

fun Module.getMappedField(mojangField: MemberReference): String {
    return namedToMojang?.tryGetMappedField(mojangField.withoutDescriptor)?.name ?: mojangField.name
}

fun Module.getMappedField(mojangClass: String, mojangField: String): String {
    return getMappedField(MemberReference(mojangField, null, mojangClass))
}

fun Module.getMojangField(mappedField: MemberReference): String {
    return namedToMojang?.getIntermediaryField(mappedField.withoutDescriptor)?.name ?: mappedField.name
}

fun Module.getMojangField(mappedClass: String, mappedField: String): String {
    return getMojangField(MemberReference(mappedField, null, mappedClass))
}

fun Module.getMojangField(field: PsiField): String? {
    val clazz = field.containingClass?.fullQualifiedName ?: return null
    return getMojangField(clazz, field.name)
}

fun Module.getMappedMethod(mojangMethod: MemberReference): String {
    return namedToMojang?.tryGetMappedMethod(mojangMethod)?.name ?: return mojangMethod.name
}

fun Module.getMappedMethod(mojangClass: String, mojangMethod: String, mojangDescriptor: String): String {
    return getMappedMethod(MemberReference(mojangMethod, mojangDescriptor, mojangClass))
}

fun Module.getMappedMethodCall(mojangClass: String, mojangMethod: String, mojangDescriptor: String, p: String): String {
    val mappedMethodRef = namedToMojang?.tryGetMappedMethod(
        MemberReference(mojangMethod, mojangDescriptor, mojangClass)
    ) ?: return "$mojangClass.$mojangMethod($p)"
    return "${mappedMethodRef.owner}.${mappedMethodRef.name}($p)"
}

fun Module.getMojangMethod(mappedMethod: MemberReference): String {
    return namedToMojang?.getIntermediaryMethod(mappedMethod)?.name ?: return mappedMethod.name
}

fun Module.getMojangMethod(mappedClass: String, mappedMethod: String, mappedDescriptor: String): String {
    return getMojangMethod(MemberReference(mappedMethod, mappedDescriptor, mappedClass))
}

fun Module.getMojangMethod(method: PsiMethod): String? {
    val clazz = method.containingClass?.fullQualifiedName ?: return null
    val descriptor = method.descriptor ?: return null
    return getMojangMethod(clazz, method.name, descriptor)
}
