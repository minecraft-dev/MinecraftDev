/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.i18n.index.TranslationIndex
import com.demonwav.mcdev.i18n.intentions.RemoveDuplicatesIntention
import com.demonwav.mcdev.i18n.intentions.RemoveUnmatchedEntryIntention
import com.demonwav.mcdev.i18n.intentions.TrimKeyIntention
import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.LangTypes
import com.demonwav.mcdev.util.mcDomain
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class LangAnnotator : Annotator {
    override fun annotate(psiElement: PsiElement, annotations: AnnotationHolder) {
        if (psiElement is LangEntry) {
            checkEntryMatchesDefault(psiElement, annotations)
            checkEntryKey(psiElement, annotations)
            checkEntryDuplicates(psiElement, psiElement.parent.children, annotations)
        }
        if (psiElement.node.elementType == LangTypes.DUMMY) {
            annotations.createErrorAnnotation(psiElement, "Translations must not contain incomplete entries.")
        }
    }

    private fun checkEntryKey(entry: LangEntry, annotations: AnnotationHolder) {
        if (entry.key != entry.trimmedKey) {
            val range = TextRange.from(entry.textRange.startOffset, entry.key.length)
            annotations.createWarningAnnotation(range, "Translation key contains whitespace at start or end.")
                .registerFix(TrimKeyIntention())
        }
    }

    private fun checkEntryDuplicates(entry: LangEntry, siblings: Array<PsiElement>, annotations: AnnotationHolder) {
        val count = siblings.count { it is LangEntry && entry.key == it.key }
        if (count > 1) {
            annotations.createWarningAnnotation(entry, "Duplicate translation keys \"${entry.key}\".")
                .registerFix(RemoveDuplicatesIntention(entry))
        }
    }

    private fun checkEntryMatchesDefault(entry: LangEntry, annotations: AnnotationHolder) {
        val domain = entry.containingFile.virtualFile.mcDomain
        val defaultEntries = TranslationIndex.getAllDefaultEntries(entry.project, domain)
        if (defaultEntries.any { it.contains(entry.key) }) {
            return
        }
        annotations.createWarningAnnotation(entry.textRange, "Translation key not included in default localization file.")
            .registerFix(RemoveUnmatchedEntryIntention())
    }
}
