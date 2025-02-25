/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.asset.MixinAssets
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SHADOW
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findFieldByName
import com.demonwav.mcdev.platform.mixin.util.findMethod
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
                    ?.let { MethodTargetMember(targetClass, it) },
            )
            is PsiField -> listOfNotNull(
                targetClass.findFieldByName(name)?.let { FieldTargetMember(targetClass, it) },
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
        val shadowTarget = MixinAnnotationHandler.resolveTarget(shadow).firstOrNull() ?: return null
        return shadowTarget.findSourceElement(member.project, member.resolveScope, canDecompile = false)
            ?.createSmartPointer()
    }

    fun findFirstShadowTargetForReference(member: PsiMember): SmartPsiElementPointer<PsiMember>? {
        val shadow = member.findAnnotation(SHADOW) ?: return null
        val shadowTarget = MixinAnnotationHandler.resolveTarget(shadow).firstOrNull() ?: return null
        return shadowTarget.findOrConstructSourceMember(member.project, member.resolveScope, canDecompile = false)
            .createSmartPointer()
    }

    private fun hasAliases(shadow: PsiAnnotation) = shadow.findDeclaredAttributeValue("aliases").isNotEmpty()

    private fun stripPrefix(shadow: PsiAnnotation, member: PsiMember): String? {
        // Strip prefix
        val prefix = shadow.findDeclaredAttributeValue("prefix")?.constantStringValue
            ?: MixinConstants.DEFAULT_SHADOW_PREFIX
        return (member.name ?: return null).removePrefix(prefix)
    }

    override val isEntryPoint = false

    override val icon = MixinAssets.MIXIN_SHADOW_ICON

    companion object {
        fun getInstance(): ShadowHandler? {
            return MixinAnnotationHandler.forMixinAnnotation(SHADOW) as? ShadowHandler
        }
    }
}
