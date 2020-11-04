/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.insight

import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.toUElementOfType

val UElement.uastEventListener: Pair<UClass, UMethod>?
    get() {
        // The PsiIdentifier is going to be a method of course!
        val method = this.uastParent as? UMethod ?: return null
        if (method.javaPsi.hasModifierProperty(PsiModifier.ABSTRACT)) {
            // I don't think any implementation allows for abstract method listeners.
            return null
        }
        val module = ModuleUtilCore.findModuleForPsiElement(this.sourcePsi ?: return null) ?: return null
        val instance = MinecraftFacet.getInstance(module) ?: return null
        // Since each platform has their own valid listener annotations,
        // some platforms may have multiple allowed annotations for various cases
        val listenerAnnotations = instance.types.flatMap { it.listenerAnnotations }
        var contains = false
        for (listenerAnnotation in listenerAnnotations) {
            if (method.findAnnotation(listenerAnnotation) != null) {
                contains = true
                break
            }
        }
        if (!contains) {
            return null
        }
        val (_, resolve) = method.uastEventParameterPair ?: return null

        if (!instance.isStaticListenerSupported(method.javaPsi) && method.isStatic) {
            return null
        }

        return resolve to method
    }

val UMethod.uastEventParameterPair: Pair<UParameter, UClass>?
    get() {
        val firstParameter = this.uastParameters.firstOrNull()
            ?: return null // Listeners must have at least a single parameter
        // Get the type of the parameter so we can start resolving it
        @Suppress("UElementAsPsi") // UVariable overrides getType so it should be fine to use on UElements...
        val type = firstParameter.type as? PsiClassType ?: return null
        // Validate that it is a class reference type
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        val resolve = type.resolve()?.toUElementOfType<UClass>() ?: return null
        return firstParameter to resolve
    }
