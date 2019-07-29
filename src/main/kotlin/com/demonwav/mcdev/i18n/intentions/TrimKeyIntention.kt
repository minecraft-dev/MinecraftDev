/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException

class TrimKeyIntention : BaseElementAtCaretIntentionAction() {
    override fun getText() = "Trim translation key"

    override fun getFamilyName() = "Minecraft"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val entry: I18nEntry = when {
            element is I18nEntry -> element
            element.node.elementType === I18nTypes.KEY && element.parent is I18nEntry -> element.parent as I18nEntry
            else -> return false
        }
        return entry.key != entry.trimmedKey
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(element.parent)) {
            return
        }
        val entry = element.parent as I18nEntry
        entry.setName(entry.trimmedKey)
    }
}
