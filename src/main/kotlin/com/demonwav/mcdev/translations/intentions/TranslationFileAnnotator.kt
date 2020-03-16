/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.demonwav.mcdev.util.mcDomain
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class TranslationFileAnnotator : Annotator {
    override fun annotate(element: PsiElement, annotations: AnnotationHolder) {
        val translation = TranslationFiles.toTranslation(element)
        if (translation != null) {
            checkEntryKey(element, translation, annotations)
            checkEntryDuplicates(element, translation, annotations)
            checkEntryMatchesDefault(element, translation, annotations)
        }
        if (element.node.elementType == LangTypes.DUMMY) {
            annotations.createErrorAnnotation(element, "Translations must not contain incomplete entries.")
        }
    }

    private fun checkEntryKey(element: PsiElement, translation: Translation, annotations: AnnotationHolder) {
        if (translation.key != translation.trimmedKey) {
            annotations.createWarningAnnotation(element, "Translation key contains whitespace at start or end.")
                .registerFix(TrimKeyIntention())
        }
    }

    private fun checkEntryDuplicates(element: PsiElement, translation: Translation, annotations: AnnotationHolder) {
        val count = TranslationIndex.getTranslations(element.containingFile).count { it.key == translation.key }
        if (count > 1) {
            annotations.createWarningAnnotation(element, "Duplicate translation keys \"${translation.key}\".")
                .registerFix(RemoveDuplicatesIntention(translation))
        }
    }

    private fun checkEntryMatchesDefault(element: PsiElement, translation: Translation, annotations: AnnotationHolder) {
        val domain = element.containingFile?.virtualFile?.mcDomain
        val defaultEntries = TranslationIndex.getAllDefaultEntries(element.project, domain)
        if (defaultEntries.any { it.contains(translation.key) }) {
            return
        }
        annotations.createWarningAnnotation(element, "Translation key not included in default localization file.")
            .registerFix(RemoveUnmatchedEntryIntention())
    }
}
