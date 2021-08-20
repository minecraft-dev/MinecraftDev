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
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.memberReference
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.refactoring.suggested.createSmartPointer
import java.util.Locale
import org.jetbrains.plugins.groovy.intentions.style.inference.resolve
import org.objectweb.asm.tree.ClassNode

fun PsiMember.findInvokerAnnotation(): PsiAnnotation? {
    return findAnnotation(MixinConstants.Annotations.INVOKER)
}

fun PsiMember.findInvokerTarget(): SmartPsiElementPointer<PsiMethod>? {
    val accessor = findInvokerAnnotation() ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    val invokerTarget = resolveInvokerTarget(accessor, targetClasses, this) ?: return null
    return invokerTarget.classAndMethod.method.findOrConstructSourceMethod(
        invokerTarget.classAndMethod.clazz,
        containingClass.project,
        containingClass.resolveScope,
        canDecompile = false
    ).createSmartPointer()
}

fun resolveInvokerTarget(
    invoker: PsiAnnotation,
    targetClasses: Collection<ClassNode>,
    member: PsiMember
): MethodTargetMember? {
    val name = getInvokerTargetName(invoker, member) ?: return null
    val constructor = name == "<init>"
    val targetMethod = when (member) {
        is PsiMethod -> targetClasses.mapFirstNotNull {
            if (constructor && member.returnType?.resolve()?.fullQualifiedName?.replace('.', '/') != it.name) {
                return null
            }
            val method = it.findMethod(member.memberReference) ?: return null
            ClassAndMethodNode(it, method)
        }
        else -> null
    } ?: return null
    return MethodTargetMember(null, targetMethod)
}

fun getInvokerTargetName(invoker: PsiAnnotation, member: PsiMember): String? {
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
    if (name.toUpperCase(Locale.ROOT) != name) {
        return name.decapitalize()
    }
    return name
}

private val PATTERN = Regex("(call|invoke|new|create)([A-Z].*?)(_\\\$md.*)?")
