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
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import java.util.Locale
import org.objectweb.asm.tree.ClassNode

fun PsiMember.findAccessorAnnotation(): PsiAnnotation? {
    return findAnnotation(MixinConstants.Annotations.ACCESSOR)
}

fun PsiMember.findAccessorTargetForReference(): SmartPsiElementPointer<PsiField>? {
    val accessor = findAccessorAnnotation() ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    val targetMember = resolveAccessorTarget(accessor, targetClasses, this) ?: return null
    return targetMember.classAndField.field.findOrConstructSourceField(
        targetMember.classAndField.clazz,
        project,
        resolveScope,
        canDecompile = false
    ).createSmartPointer()
}

fun resolveAccessorTarget(
    accessor: PsiAnnotation,
    targetClasses: Collection<ClassNode>,
    member: PsiMember
): FieldTargetMember? {
    val accessorInfo = getAccessorInfo(accessor, member) ?: return null
    return when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull { targetClass ->
            val field = targetClass.findFieldByName(accessorInfo.name)?.takeIf {
                // Accessors either have a return value (field getter) or a parameter (field setter)
                if (!member.hasParameters() && accessorInfo.type.allowGetters) {
                    it.desc == member.returnType?.descriptor
                } else if (
                    PsiType.VOID == member.returnType && member.parameterList.parametersCount == 1 &&
                    accessorInfo.type.allowSetters
                ) {
                    it.desc == member.parameterList.parameters[0].type.descriptor
                } else {
                    false
                }
            } ?: return null
            FieldTargetMember(null, ClassAndFieldNode(targetClass, field))
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
