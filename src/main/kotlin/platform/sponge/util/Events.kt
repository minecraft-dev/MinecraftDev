/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.util

import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.resolve
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.ProjectScope
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.toUElementOfType

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

fun UMethod.isValidSpongeListener(): Boolean {
    if (this.findAnnotation(SpongeConstants.LISTENER_ANNOTATION) == null ||
        this.isConstructor || this.uastParameters.isEmpty()
    ) {
        return false
    }

    val eventClass = this.uastParameters[0].typeReference?.resolve() ?: return false
    val baseEventClass = JavaPsiFacade.getInstance(this.project)
        .findClass(SpongeConstants.EVENT, ProjectScope.getAllScope(this.project)) ?: return false
    return eventClass.isInheritor(baseEventClass, true)
}

fun UMethod.resolveSpongeEventClass(): UClass? {
    val eventParam = this.uastParameters.firstOrNull() ?: return null
    return eventParam.typeReference?.resolve()
}

fun UAnnotation.resolveSpongeGetterTarget(): UMethod? {
    val method = this.getContainingUMethod() ?: return null
    val eventClass = method.resolveSpongeEventClass() ?: return null
    val getterMethodName = this.findAttributeValue("value")?.evaluateString() ?: return null
    return eventClass.findMethodsByName(getterMethodName, true)
        .firstOrNull { !it.isConstructor && !it.hasParameters() }
        .toUElementOfType()
}
