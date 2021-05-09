/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.isErasureEquivalentTo
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import java.util.Locale

fun PsiMember.findAccessorAnnotation(): PsiAnnotation? {
    return findAnnotation(MixinConstants.Annotations.ACCESSOR)
}

fun PsiMember.findAccessorTarget(): SmartPsiElementPointer<PsiMember>? {
    val accessor = findAccessorAnnotation() ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    return resolveAccessorTarget(accessor, targetClasses, this)?.createSmartPointer()
}

fun resolveAccessorTarget(
    accessor: PsiAnnotation,
    targetClasses: Collection<PsiClass>,
    member: PsiMember
): PsiMember? {
    val accessorInfo = getAccessorInfo(accessor, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull { psiClass ->
            psiClass.findFieldByName(accessorInfo.name, false)?.takeIf {
                // Accessors either have a return value (field getter) or a parameter (field setter)
                if (!member.hasParameters() && accessorInfo.type.allowGetters) {
                    it.type.isErasureEquivalentTo(member.returnType)
                } else if (
                    PsiType.VOID == member.returnType && member.parameterList.parametersCount == 1 &&
                    accessorInfo.type.allowSetters
                ) {
                    it.type.isErasureEquivalentTo(member.parameterList.parameters[0].type)
                } else {
                    false
                }
            }
        }
        else -> null
    }
}

fun getAccessorInfo(accessor: PsiAnnotation, member: PsiMember): AccessorInfo? {
    val value = accessor.findDeclaredAttributeValue("value")?.constantStringValue
    if (value != null) {
        return AccessorInfo(value, AccessorType.UNKNOWN)
    }

    val memberName = member.name ?: return null
    val result = PATTERN.matchEntire(memberName) ?: return null
    val prefix = result.groupValues[1]
    var name = result.groupValues[2]
    if (name.toUpperCase(Locale.ROOT) != name) {
        name = name.decapitalize()
    }
    val type = if (prefix == "set") {
        AccessorType.SETTER
    } else {
        AccessorType.GETTER
    }
    return AccessorInfo(name, type)
}

private val PATTERN = Regex("(get|is|set)([A-Z].*?)(_\\\$md.*)?")

data class AccessorInfo(val name: String, val type: AccessorType)

enum class AccessorType(val allowGetters: Boolean, val allowSetters: Boolean) {
    GETTER(true, false),
    SETTER(false, true),
    UNKNOWN(true, true);
}
