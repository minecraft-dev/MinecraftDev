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
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException

class RemoveUnmatchedEntryIntention : PsiElementBaseIntentionAction() {
    override fun getText() = "Remove translation"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement) = true

    override fun getFamilyName() = "Minecraft"

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        TranslationFiles.remove(TranslationFiles.seekTranslation(element) ?: return)
    }
}
