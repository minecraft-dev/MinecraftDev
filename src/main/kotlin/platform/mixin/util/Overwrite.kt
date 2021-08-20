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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.memberReference
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.createSmartPointer
import org.objectweb.asm.tree.ClassNode

val PsiMethod.isOverwrite
    get() = findAnnotation(OVERWRITE) != null

fun PsiMethod.findFirstOverwriteTarget(): SmartPsiElementPointer<PsiMethod>? {
    findAnnotation(OVERWRITE) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    val overwriteTarget = resolveFirstOverwriteTarget(targetClasses, this) ?: return null
    return overwriteTarget.method.findOrConstructSourceMethod(
        overwriteTarget.clazz,
        containingClass.project,
        containingClass.resolveScope,
        canDecompile = false
    ).createSmartPointer()
}

fun resolveFirstOverwriteTarget(targetClasses: Collection<ClassNode>, method: PsiMethod): ClassAndMethodNode? {
    return targetClasses.mapFirstNotNull { targetClass ->
        targetClass.findMethod(method.memberReference)?.let { ClassAndMethodNode(targetClass, it) }
    }
}

fun resolveOverwriteTargets(targetClasses: Collection<ClassNode>, method: PsiMethod): List<ClassAndMethodNode> {
    return targetClasses.mapNotNull { targetClass ->
        targetClass.findMethod(method.memberReference)?.let { ClassAndMethodNode(targetClass, it) }
    }
}
