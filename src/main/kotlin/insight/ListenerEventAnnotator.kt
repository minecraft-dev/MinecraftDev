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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.impl.source.PsiClassReferenceType

class ListenerEventAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!MinecraftSettings.instance.isShowEventListenerGutterIcons) {
            return
        }

        // Since we want to line up with the method declaration, not the annotation
        // declaration, we need to target identifiers, not just PsiMethods.
        val method = when (element) {
            is PsiIdentifier -> element.parent as? PsiMethod ?: return
            is PsiParameter -> element.parent.parent as? PsiMethod ?: return
            else -> return
        }

        if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
            // I don't think any implementation allows for abstract
            return
        }
        val modifierList = method.modifierList
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val instance = MinecraftFacet.getInstance(module) ?: return
        // Since each platform has their own valid listener annotations,
        // some platforms may have multiple allowed annotations for various cases
        val moduleTypes = instance.types
        var contains = false
        for (moduleType in moduleTypes) {
            val listenerAnnotations = moduleType.listenerAnnotations
            for (listenerAnnotation in listenerAnnotations) {
                if (modifierList.findAnnotation(listenerAnnotation) != null) {
                    contains = true
                    break
                }
            }
        }
        if (!contains) {
            return
        }

        val parameters = method.parameterList.parameters
        if (parameters.isEmpty()) {
            return
        }
        val eventParameter = parameters[0] // Listeners must have at least a single parameter
            ?: return
        // Get the type of the parameter so we can start resolving it
        val psiEventElement = eventParameter.typeElement ?: return
        val type = psiEventElement.type as? PsiClassReferenceType ?: return
        // Validate that it is a class reference type
        // And again, make sure that we can at least resolve the type, otherwise it's not a valid
        // class reference.
        val eventClass = type.resolve() ?: return

        if (instance.isEventClassValid(eventClass, method)) {
            return
        }

        if (!instance.isStaticListenerSupported(method) && method.hasModifierProperty(PsiModifier.STATIC)) {
            if (method.nameIdentifier != null) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Event listener method must not be static")
                    .range(method.nameIdentifier!!)
                    .create()
            }
        }

        if (element == eventParameter && !isSuperEventListenerAllowed(eventClass, method, instance)) {
            // Only annotate the first parameter
            val errorMessage = instance.writeErrorMessageForEvent(eventClass, method)
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
