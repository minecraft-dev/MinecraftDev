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

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.facet.MinecraftFacet
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UTypeReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType

class ListenerEventAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!MinecraftSettings.instance.isShowEventListenerGutterIcons) {
            return
        }

        // Since we want to line up with the method declaration, not the annotation
        // declaration, we need to target identifiers, not the whole method.
        if (element.toUElementOfType<UIdentifier>() == null) {
            return
        }

        val method: UMethod = element.toUElement()?.uastParent as? UMethod
            ?: element.getUastParentOfType<UTypeReferenceExpression>()
                ?.getParentOfType<UParameter>()?.uastParent as? UMethod // Be sure to be on the type of a parameter
            ?: return
        if (method.javaPsi.hasModifierProperty(PsiModifier.ABSTRACT)) {
            // I don't think any implementation allows for abstract
            return
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val instance = MinecraftFacet.getInstance(module) ?: return
        // Since each platform has their own valid listener annotations,
        // some platforms may have multiple allowed annotations for various cases
        val moduleTypes = instance.types
        var contains = false
        for (moduleType in moduleTypes) {
            val listenerAnnotations = moduleType.listenerAnnotations
            for (listenerAnnotation in listenerAnnotations) {
                if (method.findAnnotation(listenerAnnotation) != null) {
                    contains = true
                    break
                }
            }
        }
        if (!contains) {
            return
        }

        val eventParameter = method.uastParameters.firstOrNull() // Listeners must have at least one parameter
            ?: return
        // Validate that this is a class reference type
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        val eventClass = (eventParameter.typeReference?.type as? PsiClassType)?.resolve() ?: return
        if (instance.isEventClassValid(eventClass, method.javaPsi)) {
            return
        }

        if (!instance.isStaticListenerSupported(method.javaPsi) && method.isStatic) {
            method.javaPsi.nameIdentifier?.let {
                holder.newAnnotation(HighlightSeverity.ERROR, "Event listener method must not be static")
                    .range(it)
                    .create()
            }
        }

        if (element.getUastParentOfType<UParameter>()?.sourcePsi == eventParameter.sourcePsi &&
            !isSuperEventListenerAllowed(eventClass, method.javaPsi, instance)
        ) {
            val errorMessage = instance.writeErrorMessageForEvent(eventClass, method.javaPsi)
            if (errorMessage == null) {
                holder.newSilentAnnotation(HighlightSeverity.ERROR).create()
            } else {
                holder.newAnnotation(HighlightSeverity.ERROR, errorMessage).create()
            }
        }
    }

    private fun isSuperEventListenerAllowed(eventClass: PsiClass, method: PsiMethod, facet: MinecraftFacet): Boolean {
        val supers = eventClass.supers
        for (aSuper in supers) {
            if (facet.isEventClassValid(aSuper, method)) {
                return true
            }
            if (isSuperEventListenerAllowed(aSuper, method, facet)) {
                return true
            }
        }
        return false
    }
}
