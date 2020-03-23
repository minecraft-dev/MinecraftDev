/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
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
import org.jetbrains.annotations.Contract

@Contract(pure = true)
fun PsiMember.findAccessorTarget(): PsiMember? {
    val accessor = findAnnotation(MixinConstants.Annotations.ACCESSOR) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    return resolveAccessorTarget(accessor, targetClasses, this)
}

@Contract(pure = true)
fun resolveAccessorTarget(
        accessor: PsiAnnotation,
        targetClasses: Collection<PsiClass>,
        member: PsiMember
): PsiMember? {
    val value = accessor.findDeclaredAttributeValue("value")?.constantStringValue ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull {
            it.findFieldByName(value, false)?.takeIf {
                // Accessors either have a return value (field getter) or a parameter (field setter)
                if (!member.hasParameters()) {
                    it.type.isErasureEquivalentTo(member.returnType)
                } else if (PsiType.VOID.equals(member.returnType)) {
                    it.type.isErasureEquivalentTo(member.parameterList.parameters.get(0).type)
                } else {
                    false
                }
            }
        }
        else -> null
    }
}