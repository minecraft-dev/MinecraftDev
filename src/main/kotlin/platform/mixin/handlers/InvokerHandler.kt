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
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.decapitalize
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import com.intellij.psi.util.parentOfType
import java.util.Locale
import org.objectweb.asm.tree.ClassNode

class InvokerHandler : MixinMemberAnnotationHandler {
    companion object {
        private val PATTERN = Regex("(call|invoke|new|create)([A-Z].*?)(_\\\$md.*)?")

        fun getInstance(): InvokerHandler? {
            return MixinAnnotationHandler.forMixinAnnotation(INVOKER) as? InvokerHandler
        }
    }

    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        val member = annotation.parentOfType<PsiMethod>() ?: return emptyList()
        val name = getInvokerTargetName(annotation, member) ?: return emptyList()
        if (name == "<clinit>") {
            return emptyList()
        }

        val constructor = name == "<init>"
        if (constructor &&
            (member.returnType as? PsiClassType)?.resolve()?.fullQualifiedName?.replace('.', '/') != targetClass.name
        ) {
            return emptyList()
        }
        var wantedDesc = member.descriptor ?: return emptyList()
        if (constructor) {
            wantedDesc = wantedDesc.replaceAfterLast(')', "V")
        }
        val method = targetClass.findMethod(MemberReference(name, wantedDesc)) ?: return emptyList()
        return listOf(MethodTargetMember(targetClass, method))
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        val method = annotation.parentOfType<PsiMethod>() ?: return null
        val targetName = getInvokerTargetName(annotation, method) ?: return "Invalid invoker name ${method.name}"
        return "Cannot find method $targetName in target class"
    }

    private fun getInvokerTargetName(invoker: PsiAnnotation, member: PsiMember): String? {
        val value = invoker.findDeclaredAttributeValue("value")?.constantStringValue
        if (value != null) {
            return value
        }

        val memberName = member.name ?: return null
        val result = PATTERN.matchEntire(memberName) ?: return null
        val prefix = result.groupValues[1]
        if (prefix == "new" || prefix == "create") {
            return "<init>"
        }
        val name = result.groupValues[2]
        if (name.uppercase(Locale.ENGLISH) != name) {
            return name.decapitalize()
        }
        return name
    }

    fun findInvokerTargetForReference(member: PsiMember): SmartPsiElementPointer<PsiMethod>? {
        val accessor = member.findAnnotation(INVOKER) ?: return null
        val invokerTarget = MixinAnnotationHandler.resolveTarget(accessor).firstOrNull() as? MethodTargetMember
            ?: return null
        return invokerTarget.classAndMethod.method.findOrConstructSourceMethod(
            invokerTarget.classAndMethod.clazz,
            member.project,
            member.resolveScope,
            canDecompile = false,
        ).createSmartPointer()
    }

    override val isEntryPoint = false

    override val icon = MixinAssets.MIXIN_ACCESSOR_ICON
}
