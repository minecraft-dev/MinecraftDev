/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.intellij.codeInsight.InferredAnnotationProvider
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiModifierListOwner

class SideOnlyInferredAnnotationProvider : InferredAnnotationProvider {

    override fun findInferredAnnotation(listOwner: PsiModifierListOwner, annotationFQN: String): PsiAnnotation? {
        if (annotationFQN != SideOnlyUtil.MCDEV_SIDEONLY_ANNOTATION) {
            return null
        }
        if (SideOnlyUtil.getExplicitAnnotation(listOwner, SideHardness.EITHER) != null) {
            return null
        }
        val inferredAnnotation = SideOnlyUtil.getExplicitOrInferredAnnotation(listOwner, SideHardness.EITHER)
            ?: return null
        val annotationText =
            "@${SideOnlyUtil.MCDEV_SIDEONLY_ANNOTATION}(${SideOnlyUtil.MCDEV_SIDE}.${inferredAnnotation.side})"
        return JavaPsiFacade.getElementFactory(listOwner.project).createAnnotationFromText(annotationText, listOwner)
    }

    override fun findInferredAnnotations(listOwner: PsiModifierListOwner): MutableList<PsiAnnotation> {
        val annotation = findInferredAnnotation(listOwner, SideOnlyUtil.MCDEV_SIDEONLY_ANNOTATION)
        return if (annotation == null) {
            mutableListOf()
        } else {
            mutableListOf(annotation)
        }
    }
}
