/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.intentions.RemoveDuplicatesIntention
import com.demonwav.mcdev.i18n.intentions.RemoveUnmatchedEntryIntention
import com.demonwav.mcdev.i18n.intentions.TrimKeyIntention
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.demonwav.mcdev.util.mcDomain
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class I18nAnnotator : Annotator {
    override fun annotate(psiElement: PsiElement, annotations: AnnotationHolder) {
        if (psiElement is I18nEntry) {
            checkEntryMatchesDefault(psiElement, annotations)
            checkEntryKey(psiElement, annotations)
            checkEntryDuplicates(psiElement, psiElement.parent.children, annotations)
        }
        if (psiElement.node.elementType == I18nTypes.DUMMY) {
            annotations.newAnnotation(HighlightSeverity.ERROR, "Translations must not contain incomplete entries.")
                .create()
        }
    }

    private fun checkEntryKey(entry: I18nEntry, annotations: AnnotationHolder) {
        if (entry.key != entry.trimmedKey) {
            val range = TextRange.from(entry.textRange.startOffset, entry.key.length)
            annotations.newAnnotation(HighlightSeverity.WARNING, "Translation key contains whitespace at start or end.")
                .range(range)
                .withFix(TrimKeyIntention())
                .create()
        }
    }

    private fun checkEntryDuplicates(entry: I18nEntry, siblings: Array<PsiElement>, annotations: AnnotationHolder) {
        val count = siblings.count { it is I18nEntry && entry.key == it.key }
        if (count > 1) {
            annotations.newAnnotation(HighlightSeverity.WARNING, "Duplicate translation keys \"${entry.key}\".")
                .range(entry)
                .withFix(RemoveDuplicatesIntention(entry))
                .create()
        }
    }

    private fun checkEntryMatchesDefault(entry: I18nEntry, annotations: AnnotationHolder) {
        if (
            entry.project.findDefaultLangEntries(domain = entry.containingFile.virtualFile.mcDomain)
                .any { it.key == entry.key }
        ) {
            return
        }
        annotations.newAnnotation(
            HighlightSeverity.WARNING,
            "Translation key not included in default localization file."
        )
            .range(entry)
            .withFix(RemoveUnmatchedEntryIntention())
            .create()
    }
}
