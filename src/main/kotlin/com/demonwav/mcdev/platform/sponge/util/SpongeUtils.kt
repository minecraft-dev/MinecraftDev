/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.util

import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierListOwner

fun PsiMember.isInSpongePluginClass(): Boolean = this.containingClass?.isSpongePluginClass() == true

fun PsiClass.isSpongePluginClass(): Boolean = this.hasAnnotation(SpongeConstants.PLUGIN_ANNOTATION)

fun PsiElement.spongePluginClassId(): String? = this.findContainingClass()
    ?.getAnnotation(SpongeConstants.PLUGIN_ANNOTATION)?.findAttributeValue("id")?.constantStringValue

fun isInjected(element: PsiModifierListOwner, optionalSensitive: Boolean): Boolean {
    val annotation = element.getAnnotation(SpongeConstants.INJECT_ANNOTATION) ?: return false
    if (!optionalSensitive) {
        return true
    }

    return !isInjectOptional(annotation)
}

fun isInjectOptional(annotation: PsiAnnotation): Boolean {
    val optional = annotation.findAttribute("optional") ?: return false
    val value = optional.attributeValue
    return value is JvmAnnotationConstantValue && value.constantValue == true
}
