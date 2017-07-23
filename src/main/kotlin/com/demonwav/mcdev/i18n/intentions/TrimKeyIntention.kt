/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
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
        val property: I18nProperty = when {
            element is I18nProperty -> element
            element.node.elementType === I18nTypes.KEY && element.parent is I18nProperty -> element.parent as I18nProperty
            else -> return false
        }
        return property.key != property.trimmedKey
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(element.parent)) {
            return
        }
        val property = element.parent as I18nProperty
        property.setName(property.trimmedKey)
    }
}
