/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class TrimKeyIntention(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getText() = "Trim translation key"

    override fun getFamilyName() = "Minecraft"

    override fun isAvailable(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ): Boolean {
        val translation = TranslationFiles.toTranslation(
            TranslationFiles.seekTranslation(startElement) ?: return false,
        ) ?: return false

        return translation.key != translation.trimmedKey
    }

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val entry = TranslationFiles.seekTranslation(startElement) ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(entry)) {
            return
        }

        val translation = TranslationFiles.toTranslation(entry) ?: return
        entry.setName(translation.trimmedKey)
    }
}
