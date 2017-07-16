/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.intentions.RemoveDuplicatesIntention
import com.demonwav.mcdev.i18n.intentions.RemoveUnmatchedPropertyIntention
import com.demonwav.mcdev.i18n.intentions.TrimKeyIntention
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class I18nAnnotator : Annotator {
    override fun annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder) {
        if (psiElement is I18nProperty) {
            checkPropertyMatchesDefault(psiElement, annotationHolder)
            checkPropertyKey(psiElement, annotationHolder)
            checkPropertyDuplicates(psiElement, psiElement.parent.children, annotationHolder)
        }
    }

    private fun checkPropertyKey(property: I18nProperty, annotations: AnnotationHolder) {
        if (property.key != property.key.trim { it <= ' ' }) {
            annotations.createWarningAnnotation(TextRange(property.textRange.startOffset,
                property.textRange.startOffset + property.key.length),
                "Translation key contains whitespace at start or end").registerFix(TrimKeyIntention())
        }
    }

    private fun checkPropertyDuplicates(property: I18nProperty, siblings: Array<PsiElement>, annotations: AnnotationHolder) {
        val count = siblings.count { it is I18nProperty && property.key == it.key }
        if (count > 1) {
            annotations.createWarningAnnotation(property,
                "Duplicate translation keys \"" + property.key + "\"").registerFix(
                RemoveDuplicatesIntention(property))
        }
    }

    private fun checkPropertyMatchesDefault(property: I18nProperty, annotations: AnnotationHolder) {
        for (prop in property.project.findDefaultProperties(domain = I18nElementFactory.getResourceDomain(property.containingFile.virtualFile))) {
            if (prop.key == property.key) {
                return
            }
        }
        annotations.createWarningAnnotation(property.textRange,
            "Translation key not included in default localization file")
            .registerFix(RemoveUnmatchedPropertyIntention())
    }
}
