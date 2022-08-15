/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.translations.TranslationFiles
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException

class TrimKeyIntention : PsiElementBaseIntentionAction() {
    override fun getText() = "Trim translation key"

    override fun getFamilyName() = "Minecraft"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val translation = TranslationFiles.toTranslation(
            TranslationFiles.seekTranslation(element) ?: return false
        ) ?: return false

        return translation.key != translation.trimmedKey
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val entry = TranslationFiles.seekTranslation(element) ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(entry)) {
            return
        }

        val translation = TranslationFiles.toTranslation(entry) ?: return
        entry.setName(translation.trimmedKey)
    }
}
