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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.DEFAULT_SHADOW_PREFIX
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findMatchingField
import com.demonwav.mcdev.util.findMatchingMethod
import com.demonwav.mcdev.util.findMatchingMethods
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.isNotEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract

@get:Contract(pure = true)
val PsiMember.isShadow
    get() = findAnnotation(SHADOW) != null

fun PsiMember.findFirstShadowTarget(): PsiMember? {
    val shadow = findAnnotation(SHADOW) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    return resolveFirstShadowTarget(shadow, targetClasses, this)
}

fun resolveFirstShadowTarget(
    shadow: PsiAnnotation,
    targetClasses: Collection<PsiClass>,
    member: PsiMember
): PsiMember? {
    if (hasAliases(shadow)) return null
    val name = stripPrefix(shadow, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull { it.findMatchingMethod(member, false, name) }
        is PsiField -> targetClasses.mapFirstNotNull { it.findMatchingField(member, false, name) }
        else -> null
    }
}

fun PsiMember.findShadowTargets(): List<PsiMember> {
    val shadow = findAnnotation(SHADOW) ?: return emptyList()
    val containingClass = containingClass ?: return emptyList()
    val targetClasses = containingClass.mixinTargets.ifEmpty { return emptyList() }
    return resolveShadowTargets(shadow, targetClasses, this) ?: emptyList()
}

fun resolveShadowTargets(
    shadow: PsiAnnotation,
    targetClasses: Collection<PsiClass>,
    member: PsiMember
): List<PsiMember>? {
    if (hasAliases(shadow)) return null
    val name = stripPrefix(shadow, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.flatMap { it.findMatchingMethods(member, false, name) }
        is PsiField -> targetClasses.mapNotNull { it.findMatchingField(member, false, name) }
        else -> null
    }
}

private fun hasAliases(shadow: PsiAnnotation): Boolean = shadow.findDeclaredAttributeValue("aliases").isNotEmpty()

private fun stripPrefix(shadow: PsiAnnotation, member: PsiMember): String? {
    // Strip prefix
    val prefix = shadow.findDeclaredAttributeValue("prefix")?.constantStringValue ?: DEFAULT_SHADOW_PREFIX
    return (member.name ?: return null).removePrefix(prefix)
}
