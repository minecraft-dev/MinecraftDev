/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.index.TranslationIndex
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.demonwav.mcdev.util.mcDomain
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
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
            annotations.newAnnotation(HighlightSeverity.ERROR, MCDevBundle("translations.annotator.incomplete_entry"))
                .range(element)
                .create()
        }
    }

    private fun checkEntryKey(element: PsiElement, translation: Translation, annotations: AnnotationHolder) {
        if (translation.key != translation.trimmedKey) {
            annotations.newAnnotation(HighlightSeverity.WARNING, MCDevBundle("translations.annotator.untrimmed_key"))
                .range(element)
                .newFix(TrimKeyIntention(element)).universal().registerFix()
                .create()
        }
    }

    private fun checkEntryDuplicates(element: PsiElement, translation: Translation, annotations: AnnotationHolder) {
        val count = TranslationIndex.Util.getTranslations(element.containingFile).count { it.key == translation.key }
        if (count > 1) {
            annotations.newAnnotation(
                HighlightSeverity.WARNING,
                MCDevBundle("translations.annotator.duplicate_key", translation.key),
            )
                .newFix(RemoveDuplicatesIntention(translation, element)).universal().registerFix()
                .create()
        }
    }

    private fun checkEntryMatchesDefault(element: PsiElement, translation: Translation, annotations: AnnotationHolder) {
        val domain = element.containingFile?.virtualFile?.mcDomain
        val defaultEntries = TranslationIndex.Util.getAllDefaultEntries(element.project, domain)
        if (defaultEntries.any { it.contains(translation.key) }) {
            return
        }
        val warningText = MCDevBundle("translations.annotator.not_in_default")
        annotations.newAnnotation(HighlightSeverity.WARNING, warningText)
            .newFix(RemoveUnmatchedEntryIntention(element)).universal().registerFix()
            .create()
    }
}
