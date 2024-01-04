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

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.index.TranslationInverseIndex
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

class RemoveDuplicatesIntention(private val translation: Translation) : PsiElementBaseIntentionAction() {
    override fun getText() = "Remove duplicates (keep this translation)"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) = true

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val keep = TranslationFiles.seekTranslation(element) ?: return
        val entries = TranslationInverseIndex.findElements(
            translation.key,
            GlobalSearchScope.fileScope(element.containingFile),
        )
        for (other in entries) {
            if (other !== keep) {
                TranslationFiles.remove(other)
            }
        }
    }
}
