/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findFieldByName
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceField
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.findSourceField
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.isNotEmpty
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.tree.ClassNode

class ShadowHandler : MixinMemberAnnotationHandler {
    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        if (hasAliases(annotation)) return emptyList()
        val member = annotation.parentOfType<PsiMember>() ?: return emptyList()
        val name = stripPrefix(annotation, member) ?: return emptyList()
        return when (member) {
            is PsiMethod -> listOfNotNull(
                targetClass.findMethod(MemberReference(name, member.descriptor))
                    ?.let { MethodTargetMember(targetClass, it) }
            )
            is PsiField -> listOfNotNull(
                targetClass.findFieldByName(name)?.let { FieldTargetMember(targetClass, it) }
            )
            else -> emptyList()
        }
    }

    override fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): InsnResolutionInfo.Failure? {
        if (hasAliases(annotation)) {
            return null
        }
        return super.isUnresolved(annotation, targetClass)
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        val member = annotation.parentOfType<PsiMember>() ?: return null
        val type = when (member) {
            is PsiMethod -> "method"
            is PsiField -> "field"
            else -> return null
        }
        return "Unresolved $type ${member.name} in target class"
    }

    fun findFirstShadowTargetForNavigation(member: PsiMember): SmartPsiElementPointer<PsiElement>? {
        val shadow = member.findAnnotation(SHADOW) ?: return null
        val shadowTarget = resolveTarget(shadow).firstOrNull() ?: return null
        return when (shadowTarget) {
            is FieldTargetMember -> shadowTarget.classAndField.field.findSourceField(
                shadowTarget.classAndField.clazz,
                member.project,
                member.resolveScope,
                canDecompile = false
            )
            is MethodTargetMember -> shadowTarget.classAndMethod.method.findSourceElement(
                shadowTarget.classAndMethod.clazz,
                member.project,
                member.resolveScope,
                canDecompile = false
            )
        }?.createSmartPointer()
    }

    fun findFirstShadowTargetForReference(member: PsiMember): SmartPsiElementPointer<PsiMember>? {
        val shadow = member.findAnnotation(SHADOW) ?: return null
        val shadowTarget = resolveTarget(shadow).firstOrNull() ?: return null
        return when (shadowTarget) {
            is FieldTargetMember -> shadowTarget.classAndField.field.findOrConstructSourceField(
                shadowTarget.classAndField.clazz,
                member.project,
                member.resolveScope,
                canDecompile = false
            )
            is MethodTargetMember -> shadowTarget.classAndMethod.method.findOrConstructSourceMethod(
                shadowTarget.classAndMethod.clazz,
                member.project,
                member.resolveScope,
                canDecompile = false
            )
        }.createSmartPointer()
    }

    private fun hasAliases(shadow: PsiAnnotation) = shadow.findDeclaredAttributeValue("aliases").isNotEmpty()

    private fun stripPrefix(shadow: PsiAnnotation, member: PsiMember): String? {
        // Strip prefix
        val prefix = shadow.findDeclaredAttributeValue("prefix")?.constantStringValue
            ?: MixinConstants.DEFAULT_SHADOW_PREFIX
        return (member.name ?: return null).removePrefix(prefix)
    }

    override val isEntryPoint = false

    companion object {
        fun getInstance(): ShadowHandler? {
            return MixinAnnotationHandler.forMixinAnnotation(SHADOW) as? ShadowHandler
        }
    }
}
