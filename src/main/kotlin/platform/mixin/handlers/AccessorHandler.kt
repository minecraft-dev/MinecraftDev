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
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findFieldByName
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceField
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.decapitalize
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import com.intellij.psi.util.parentOfType
import java.util.Locale
import org.objectweb.asm.tree.ClassNode

class AccessorHandler : MixinMemberAnnotationHandler {
    companion object {
        private val PATTERN = Regex("(get|is|set)([A-Z].*?)(_\\\$md.*)?")

        fun getInstance(): AccessorHandler? {
            return MixinAnnotationHandler.forMixinAnnotation(ACCESSOR) as? AccessorHandler
        }
    }

    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        val method = annotation.parentOfType<PsiMethod>() ?: return emptyList()
        val accessorInfo = getAccessorInfo(annotation, method) ?: return emptyList()
        val field = targetClass.findFieldByName(accessorInfo.name)?.takeIf {
            // Accessors either have a return value (field getter) or a parameter (field setter)
            if (!method.hasParameters() && accessorInfo.type.allowGetters) {
                it.desc == method.returnType?.descriptor
            } else if (
                PsiTypes.voidType() == method.returnType && method.parameterList.parametersCount == 1 &&
                accessorInfo.type.allowSetters
            ) {
                it.desc == method.parameterList.parameters[0].type.descriptor
            } else {
                false
            }
        } ?: return emptyList()
        return listOf(FieldTargetMember(targetClass, field))
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        val method = annotation.parentOfType<PsiMethod>() ?: return null
        val accessorInfo = getAccessorInfo(annotation, method) ?: return "Invalid accessor name ${method.name}"
        return "Cannot find field ${accessorInfo.name} in target class"
    }

    private fun getAccessorInfo(accessor: PsiAnnotation, member: PsiMember): AccessorInfo? {
        val value = accessor.findDeclaredAttributeValue("value")?.constantStringValue
        if (value != null) {
            return AccessorInfo(value, AccessorType.UNKNOWN)
        }

        val memberName = member.name ?: return null
        val result = PATTERN.matchEntire(memberName) ?: return null
        val prefix = result.groupValues[1]
        var name = result.groupValues[2]
        if (name.uppercase(Locale.ENGLISH) != name || name.length == 1) {
            name = name.decapitalize()
        }
        val type = if (prefix == "set") {
            AccessorType.SETTER
        } else {
            AccessorType.GETTER
        }
        return AccessorInfo(name, type)
    }

    fun findAccessorTargetForReference(method: PsiMethod): SmartPsiElementPointer<PsiField>? {
        val accessor = method.findAnnotation(ACCESSOR) ?: return null
        val targetMember = MixinAnnotationHandler.resolveTarget(accessor).firstOrNull() as? FieldTargetMember
            ?: return null
        return targetMember.classAndField.field.findOrConstructSourceField(
            targetMember.classAndField.clazz,
            method.project,
            method.resolveScope,
            canDecompile = false,
        ).createSmartPointer()
    }

    override val isEntryPoint = false

    override val icon = MixinAssets.MIXIN_ACCESSOR_ICON

    data class AccessorInfo(val name: String, val type: AccessorType)

    enum class AccessorType(val allowGetters: Boolean, val allowSetters: Boolean) {
        GETTER(true, false),
        SETTER(false, true),
        UNKNOWN(true, true),
        ;
    }
}
