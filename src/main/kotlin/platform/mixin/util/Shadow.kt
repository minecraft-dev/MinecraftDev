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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.DEFAULT_SHADOW_PREFIX
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.isNotEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import org.objectweb.asm.tree.ClassNode

val PsiMember.isShadow
    get() = findAnnotation(SHADOW) != null

fun PsiMember.findFirstShadowTargetForNavigation(): SmartPsiElementPointer<PsiElement>? {
    val shadow = findAnnotation(SHADOW) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    val shadowTarget = resolveFirstShadowTarget(shadow, targetClasses, this) ?: return null
    return when (shadowTarget) {
        is FieldTargetMember -> shadowTarget.classAndField.field.findSourceField(
            shadowTarget.classAndField.clazz,
            containingClass.project,
            containingClass.resolveScope,
            canDecompile = false
        )
        is MethodTargetMember -> shadowTarget.classAndMethod.method.findSourceElement(
            shadowTarget.classAndMethod.clazz,
            containingClass.project,
            containingClass.resolveScope,
            canDecompile = false
        )
    }?.createSmartPointer()
}

fun PsiMember.findFirstShadowTargetForReference(): SmartPsiElementPointer<PsiMember>? {
    val shadow = findAnnotation(SHADOW) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    val shadowTarget = resolveFirstShadowTarget(shadow, targetClasses, this) ?: return null
    return when (shadowTarget) {
        is FieldTargetMember -> shadowTarget.classAndField.field.findOrConstructSourceField(
            shadowTarget.classAndField.clazz,
            containingClass.project,
            containingClass.resolveScope,
            canDecompile = false
        )
        is MethodTargetMember -> shadowTarget.classAndMethod.method.findOrConstructSourceMethod(
            shadowTarget.classAndMethod.clazz,
            containingClass.project,
            containingClass.resolveScope,
            canDecompile = false
        )
    }.createSmartPointer()
}

fun resolveFirstShadowTarget(
    shadow: PsiAnnotation,
    targetClasses: Collection<ClassNode>,
    member: PsiMember
): MixinTargetMember? {
    if (hasAliases(shadow)) return null
    val name = stripPrefix(shadow, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull { targetClass ->
            targetClass.findMethod(MemberReference(name, member.descriptor))
                ?.let { MethodTargetMember(null, ClassAndMethodNode(targetClass, it)) }
        }
        is PsiField -> targetClasses.mapFirstNotNull { targetClass ->
            targetClass.findFieldByName(name)?.let { FieldTargetMember(null, ClassAndFieldNode(targetClass, it)) }
        }
        else -> null
    }
}

fun PsiMember.findShadowTargets(): List<MixinTargetMember> {
    val shadow = findAnnotation(SHADOW) ?: return emptyList()
    val containingClass = containingClass ?: return emptyList()
    val targetClasses = containingClass.mixinTargets.ifEmpty { return emptyList() }
    return resolveShadowTargets(shadow, targetClasses, this) ?: emptyList()
}

fun resolveShadowTargets(
    shadow: PsiAnnotation,
    targetClasses: Collection<ClassNode>,
    member: PsiMember
): List<MixinTargetMember>? {
    if (hasAliases(shadow)) return null
    val name = stripPrefix(shadow, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapNotNull { targetClass ->
            targetClass.findMethod(MemberReference(name, member.descriptor))
                ?.let { MethodTargetMember(null, ClassAndMethodNode(targetClass, it)) }
        }
        is PsiField -> targetClasses.mapNotNull { targetClass ->
            targetClass.findFieldByName(name)?.let { FieldTargetMember(null, ClassAndFieldNode(targetClass, it)) }
        }
        else -> null
    }
}

private fun hasAliases(shadow: PsiAnnotation): Boolean = shadow.findDeclaredAttributeValue("aliases").isNotEmpty()

private fun stripPrefix(shadow: PsiAnnotation, member: PsiMember): String? {
    // Strip prefix
    val prefix = shadow.findDeclaredAttributeValue("prefix")?.constantStringValue ?: DEFAULT_SHADOW_PREFIX
    return (member.name ?: return null).removePrefix(prefix)
}
