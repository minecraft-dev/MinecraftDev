/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.LangTypes
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
        val entry: LangEntry = when {
            element is LangEntry -> element
            element.node.elementType === LangTypes.KEY && element.parent is LangEntry -> element.parent as LangEntry
            else -> return false
        }
        return entry.key != entry.trimmedKey
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(element.parent)) {
            return
        }
        val entry = element.parent as LangEntry
        entry.setName(entry.trimmedKey)
    }
}
