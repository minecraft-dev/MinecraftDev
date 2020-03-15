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

import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findContainingMethod
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScope

fun PsiMethod.isValidSpongeListener(): Boolean {
    if (!this.hasAnnotation(SpongeConstants.LISTENER_ANNOTATION) || this.isConstructor || !this.hasParameters()) {
        return false
    }

    val eventClass = (this.parameters[0].type as? JvmReferenceType)?.resolve() as? PsiClass ?: return false
    val baseEventClass = JavaPsiFacade.getInstance(this.project)
        .findClass(SpongeConstants.EVENT, ProjectScope.getAllScope(this.project)) ?: return false

    return eventClass.isInheritor(baseEventClass, true)
}

fun PsiMethod.resolveSpongeEventClass(): PsiClass? {
    if (this.parameters.isEmpty()) {
        return null
    }

    return ((this.parameters[0].type as JvmReferenceType).resolve() as? PsiClass)
}

fun PsiAnnotation.resolveSpongeGetterTarget(): PsiMethod? {
    val method = this.findContainingMethod() ?: return null
    val eventClass = method.resolveSpongeEventClass() ?: return null
    val getterMethodName = this.findAttributeValue("value")?.constantValue?.toString() ?: return null
    return eventClass.findMethodsByName(getterMethodName, true).firstOrNull { !it.isConstructor && !it.hasParameters() }
}
