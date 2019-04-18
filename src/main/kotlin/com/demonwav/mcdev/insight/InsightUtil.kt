/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.impl.source.PsiClassReferenceType

val PsiElement.eventListener: Pair<PsiClass, PsiMethod>?
    get() {
        // Since we want to line up with the method declaration, not the annotation
        // declaration, we need to target identifiers, not just PsiMethods.
        if (!(this is PsiIdentifier && this.getParent() is PsiMethod)) {
            return null
        }
        // The PsiIdentifier is going to be a method of course!
        val method = this.getParent() as PsiMethod
        if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
            // I don't think any implementation allows for abstract method listeners.
            return null
        }
        val modifierList = method.modifierList
        val module = ModuleUtilCore.findModuleForPsiElement(this) ?: return null
        val instance = MinecraftFacet.getInstance(module) ?: return null
        // Since each platform has their own valid listener annotations,
        // some platforms may have multiple allowed annotations for various cases
        val listenerAnnotations = instance.types.flatMap { it.listenerAnnotations }
        var contains = false
        for (listenerAnnotation in listenerAnnotations) {
            if (modifierList.findAnnotation(listenerAnnotation) != null) {
                contains = true
                break
            }
        }
        if (!contains) {
            return null
        }
        val parameters = method.parameterList.parameters
        if (parameters.isEmpty()) {
            return null
        }
        val psiParameter = parameters[0] ?: // Listeners must have at least a single parameter
            return null
        // Get the type of the parameter so we can start resolving it
        val psiEventElement = psiParameter.typeElement ?: return null
        val type = psiEventElement.type as? PsiClassReferenceType ?: return null
        // Validate that it is a class reference type
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        val resolve = type.resolve() ?: return null

        if (!instance.isStaticListenerSupported(method) && method.hasModifierProperty(PsiModifier.STATIC)) {
            return null
        }

        return resolve to method
    }

val PsiMethod.eventParameterPair: Pair<PsiParameter, PsiClass>?
    get() {
        val parameters = this.parameterList.parameters
        if (parameters.isEmpty()) {
            return null
        }
        val psiParameter = parameters[0] ?: // Listeners must have at least a single parameter
            return null
        // Get the type of the parameter so we can start resolving it
        val psiEventElement = psiParameter.typeElement ?: return null
        val type = psiEventElement.type as? PsiClassReferenceType ?: return null
        // Validate that it is a class reference type
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        val resolve = type.resolve() ?: return null
        return psiParameter to resolve
    }
