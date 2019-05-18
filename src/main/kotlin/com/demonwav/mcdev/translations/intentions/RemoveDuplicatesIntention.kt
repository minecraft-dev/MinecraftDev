/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.translations.Translation
import com.demonwav.mcdev.translations.TranslationFiles
import com.demonwav.mcdev.translations.index.TranslationInverseIndex
import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.demonwav.mcdev.translations.lang.gen.psi.LangTypes
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException

class RemoveDuplicatesIntention(private val translation: Translation) : PsiElementBaseIntentionAction() {
    override fun getText() = "Remove duplicates (keep this translation)"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement) = true

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val keep = TranslationFiles.seekTranslation(element) ?: return
        val entries = TranslationInverseIndex.findElements(translation.key, GlobalSearchScope.fileScope(element.containingFile))
        for (other in entries) {
            if (other !== keep) {
                TranslationFiles.remove(other)
            }
        }
    }
}

