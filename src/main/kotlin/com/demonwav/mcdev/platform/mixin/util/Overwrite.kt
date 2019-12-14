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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findMatchingMethod
import com.demonwav.mcdev.util.findMatchingMethods
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.mapFirstNotNull
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.annotations.Contract

@get:Contract(pure = true)
val PsiMethod.isOverwrite
    get() = findAnnotation(OVERWRITE) != null

fun PsiMethod.findFirstOverwriteTarget(): PsiMethod? {
    findAnnotation(OVERWRITE) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    return resolveFirstOverwriteTarget(targetClasses, this)
}

fun resolveFirstOverwriteTarget(targetClasses: Collection<PsiClass>, method: PsiMethod): PsiMethod? {
    return targetClasses.mapFirstNotNull { it.findMatchingMethod(method, false) }
}

fun PsiMethod.findOverwriteTargets(): List<PsiMethod>? {
    findAnnotation(OVERWRITE) ?: return null
    val containingClass = containingClass ?: return null
    val targetClasses = containingClass.mixinTargets.ifEmpty { return null }
    return resolveOverwriteTargets(targetClasses, this)
}

fun resolveOverwriteTargets(targetClasses: Collection<PsiClass>, method: PsiMethod): List<PsiMethod> {
    return targetClasses.flatMap { it.findMatchingMethods(method, false) }
}
